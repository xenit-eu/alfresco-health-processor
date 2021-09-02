package eu.xenit.alfresco.healthprocessor.reporter;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import java.util.Collections;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class SummaryLoggingHealthReporterTest {

    private static final NodeHealthReport REPORT_1 = TestReports.healthy();
    private static final NodeHealthReport REPORT_2 = TestReports.unhealthy();

    @Test
    void smokeTest() {
        SummaryLoggingHealthReporter reporter = new SummaryLoggingHealthReporter();

        assertThat("ToggleableReporter is disabled by default", reporter.isEnabled(), is(false));

        EnumMap<NodeHealthStatus, Long> counts = new EnumMap<>(NodeHealthStatus.class);
        counts.put(NodeHealthStatus.HEALTHY, 1L);
        counts.put(NodeHealthStatus.UNHEALTHY, 1L);

        ProcessorPluginOverview overview = new ProcessorPluginOverview(
                AssertHealthProcessorPlugin.class,
                counts,
                Collections.singletonList(REPORT_2));

        reporter.onStart();
        reporter.processReports(AssertHealthProcessorPlugin.class, set(REPORT_1, REPORT_2));
        reporter.onProgress(NullCycleProgress.getInstance());
        reporter.onCycleDone(Collections.singletonList(overview));
    }

}
