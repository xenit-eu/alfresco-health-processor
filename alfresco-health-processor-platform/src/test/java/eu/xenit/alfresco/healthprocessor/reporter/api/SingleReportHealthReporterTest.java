package eu.xenit.alfresco.healthprocessor.reporter.api;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.TestReports;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SingleReportHealthReporterTest {

    private static final NodeHealthReport REPORT_1 = TestReports.healthy();
    private static final NodeHealthReport REPORT_2 = TestReports.unhealthy();

    @Test
    void toggleable() {
        TestReporter reporter = new TestReporter();

        reporter.setEnabled(true);
        assertThat(reporter.isEnabled(), is(true));

        reporter.setEnabled(false);
        assertThat(reporter.isEnabled(), is(false));
    }

    @Test
    void divideAndConquer() {
        TestReporter reporter = new TestReporter();

        reporter.processReports(AssertHealthProcessorPlugin.class, set(REPORT_1, REPORT_2));

        assertThat(reporter.invocations, containsInAnyOrder(REPORT_1, REPORT_2));
    }

    @Test
    void divideAndConquer_filterOnStatus() {
        TestReporter reporter = new TestReporter();

        reporter.statusesToHandle = set(NodeHealthStatus.UNHEALTHY);

        reporter.processReports(AssertHealthProcessorPlugin.class, set(REPORT_1, REPORT_2));

        assertThat(reporter.invocations, containsInAnyOrder(REPORT_2));
    }

    static class TestReporter extends SingleReportHealthReporter {

        Set<NodeHealthStatus> statusesToHandle;
        List<NodeHealthReport> invocations = new ArrayList<>();

        @Override
        protected Set<NodeHealthStatus> statusesToHandle() {
            return statusesToHandle == null ? super.statusesToHandle() : statusesToHandle;
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onCycleDone(List<ProcessorPluginOverview> overviews) {

        }

        @Override
        protected void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass) {
            invocations.add(report);
        }
    }

}