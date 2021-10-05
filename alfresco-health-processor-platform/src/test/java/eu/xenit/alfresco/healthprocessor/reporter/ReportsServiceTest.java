package eu.xenit.alfresco.healthprocessor.reporter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.store.HealthReportsStore;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    private static final NodeHealthReport REPORT_1 = TestReports.healthy();
    private static final NodeHealthReport REPORT_2 = TestReports.unhealthy();

    private static final Set<NodeHealthReport> REPORT_SET = new HashSet<NodeHealthReport>() {{
        add(REPORT_1);
        add(REPORT_2);
    }};

    private ReportsService service;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private HealthReportsStore reportsStore;
    @Mock
    private HealthReporter healthReporter;

    @BeforeEach
    void setup() {
        when(healthReporter.isEnabled()).thenReturn(true);

        service = new ReportsService(reportsStore, Arrays.asList(reportsStore, healthReporter));
    }

    @Test
    void onStart_delegateToReportsStoreAndReporters() {
        service.onStart();

        verify(healthReporter).onStart();
        verify(reportsStore).onStart();
    }

    @Test
    void processReports_delegateToReportsStoreAndReporters() {
        service.processReports(AssertHealthProcessorPlugin.class, REPORT_SET);

        verify(healthReporter).processReports(AssertHealthProcessorPlugin.class, REPORT_SET);
        verify(reportsStore).processReports(AssertHealthProcessorPlugin.class, REPORT_SET);
    }

    @Test
    void processReports_pluginDisabled() {
        when(healthReporter.isEnabled()).thenReturn(false);

        service.processReports(AssertHealthProcessorPlugin.class, REPORT_SET);

        verify(healthReporter, never()).processReports(any(), any());
        verify(reportsStore).processReports(AssertHealthProcessorPlugin.class, REPORT_SET);
    }

    @Test
    void onException_delegateToReportsStoreAndReporters() {
        service.onException(new VerySpecificException("JIKES"));

        verify(healthReporter).onException(any(VerySpecificException.class));
    }

    @Test
    void onProgress() {
        service.onProgress(new SimpleCycleProgress(0, 1, () -> 1));

        verify(healthReporter).onProgress(any(SimpleCycleProgress.class));
    }

    @Test
    void onCycleDone() {
        Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> reports = new HashMap<>();
        reports.put(AssertHealthProcessorPlugin.class, Collections.singletonList(REPORT_2));

        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> stats = new HashMap<>();
        Map<NodeHealthStatus, Long> countByStatus = new HashMap<>();
        countByStatus.put(NodeHealthStatus.UNHEALTHY, 1L);
        countByStatus.put(NodeHealthStatus.HEALTHY, 1L);
        stats.put(AssertHealthProcessorPlugin.class, countByStatus);

        when(reportsStore.retrieveStoredReports()).thenReturn(reports);
        when(reportsStore.retrieveRecordedStats()).thenReturn(stats);

        service.onCycleDone();

        ArgumentCaptor<List<ProcessorPluginOverview>> captor = ArgumentCaptor.forClass(List.class);
        verify(healthReporter).onCycleDone(captor.capture());
        List<ProcessorPluginOverview> invocationArgument = captor.getValue();
        assertThat(invocationArgument, hasSize(1));
        assertThat(invocationArgument.get(0).getPluginClass(), is(equalTo(AssertHealthProcessorPlugin.class)));
        assertThat(invocationArgument.get(0).getReports(), hasSize(1));
        assertThat(invocationArgument.get(0).getReports().get(0), is(equalTo(REPORT_2)));

        verify(reportsStore).onCycleDone(Mockito.any());
    }

    private static class VerySpecificException extends RuntimeException {

        public VerySpecificException(String message) {
            super(message);
        }
    }

}
