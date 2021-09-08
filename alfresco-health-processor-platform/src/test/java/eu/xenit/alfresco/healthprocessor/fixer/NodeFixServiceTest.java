package eu.xenit.alfresco.healthprocessor.fixer;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NodeFixServiceTest {

    private AssertHealthFixerPlugin healthFixerPlugin;
    private ReportEmittingHealthFixerPlugin reportEmitterPlugin;

    private NodeFixServiceBuilder builder;


    @BeforeEach
    void setup() {
        AssertTransactionHelper transactionHelper = new AssertTransactionHelper();
        healthFixerPlugin = new AssertHealthFixerPlugin();
        reportEmitterPlugin = new ReportEmittingHealthFixerPlugin();
        builder = NodeFixServiceBuilder.create()
                .transactionHelper(transactionHelper)
                .fixer(healthFixerPlugin)
                .fixer(reportEmitterPlugin);
    }


    @Test
    void fixUnhealthyNodes_noUnhealthy() {
        Set<NodeHealthReport> inputHealthReports = set(
                TestReports.healthy(),
                TestReports.healthy()
        );
        Set<NodeHealthReport> healthReportSet = builder.build()
                .fixUnhealthyNodes(AssertHealthProcessorPlugin.class, inputHealthReports);

        assertThat(healthReportSet, is(equalTo(healthReportSet)));

        healthFixerPlugin.expectNoInvocation();
    }

    @Test
    void fixUnhealthyNodes_someUnhealthy() {
        NodeHealthReport unhealthy = TestReports.unhealthy();
        Set<NodeHealthReport> inputHealthReports = set(
                TestReports.healthy(),
                unhealthy
        );
        Set<NodeHealthReport> healthReportSet = builder.build()
                .fixUnhealthyNodes(AssertHealthProcessorPlugin.class, inputHealthReports);

        assertThat(healthReportSet, is(equalTo(healthReportSet)));

        healthFixerPlugin.expectInvocation(unhealthy);

        assertThat(unhealthy.data(NodeFixReport.class), is(empty()));
    }

    public static Stream<Arguments> fixReportsFactory() {
        return Stream.of(
                // Simple ones with one fix report
                Arguments.arguments(Collections.singletonList(NodeFixStatus.FAILED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(Collections.singletonList(NodeFixStatus.SKIPPED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(Collections.singletonList(NodeFixStatus.SUCCEEDED), NodeHealthStatus.FIXED),
                // Combinations of multiple fix reports
                Arguments.arguments(asList(NodeFixStatus.FAILED, NodeFixStatus.FAILED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(asList(NodeFixStatus.FAILED, NodeFixStatus.SUCCEEDED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(asList(NodeFixStatus.FAILED, NodeFixStatus.SKIPPED), NodeHealthStatus.UNHEALTHY),

                Arguments.arguments(asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.FAILED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SUCCEEDED), NodeHealthStatus.FIXED),
                Arguments.arguments(asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SKIPPED), NodeHealthStatus.FIXED),

                Arguments.arguments(asList(NodeFixStatus.SKIPPED, NodeFixStatus.FAILED), NodeHealthStatus.UNHEALTHY),
                Arguments.arguments(asList(NodeFixStatus.SKIPPED, NodeFixStatus.SUCCEEDED), NodeHealthStatus.FIXED),
                Arguments.arguments(asList(NodeFixStatus.SKIPPED, NodeFixStatus.SKIPPED), NodeHealthStatus.UNHEALTHY)
        );
    }

    @ParameterizedTest(name = ParameterizedTest.DISPLAY_NAME_PLACEHOLDER + " "
            + ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
    @MethodSource("fixReportsFactory")
    void fixUnhealthyNodes_processFixReports(List<NodeFixStatus> fixStatus, NodeHealthStatus healthStatus) {
        NodeHealthReport unhealthy = TestReports.unhealthy();
        Set<NodeHealthReport> inputHealthReports = set(
                TestReports.healthy(),
                unhealthy
        );

        Set<NodeFixReport> fixReports = fixStatus.stream()
                .map(status -> new NodeFixReport(status, unhealthy)).collect(
                        Collectors.toSet());

        reportEmitterPlugin.scheduleFixReports(fixReports);

        Set<NodeHealthReport> healthReports = builder
                .build()
                .fixUnhealthyNodes(AssertHealthProcessorPlugin.class, inputHealthReports);

        Optional<NodeHealthReport> revisedReport = findSimilarReport(healthReports, unhealthy);
        assertThat(revisedReport.map(NodeHealthReport::getStatus), is(equalTo(Optional.of(healthStatus))));
        assertThat(getFixReports(revisedReport), is(equalTo(fixReports)));
    }


    private Optional<NodeHealthReport> findSimilarReport(Set<NodeHealthReport> reports, NodeHealthReport report) {
        return reports.stream()
                .filter(r -> r.getNodeRef().equals(report.getNodeRef()))
                .findAny();
    }

    private Set<NodeFixReport> getFixReports(Optional<NodeHealthReport> healthReport) {
        return healthReport.get().data(NodeFixReport.class);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    private static class NodeFixServiceBuilder {

        static NodeFixServiceBuilder create() {
            return new NodeFixServiceBuilder();
        }

        private TransactionHelper transactionHelper;
        private List<HealthFixerPlugin> fixers;

        NodeFixServiceBuilder fixer(HealthFixerPlugin fixer) {
            if (fixers == null) {
                fixers = new ArrayList<>();
            }
            fixers.add(fixer);
            return this;
        }

        NodeFixService build() {
            return new NodeFixService(fixers, transactionHelper);
        }

    }

}
