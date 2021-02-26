package eu.xenit.alfresco.healthprocessor.reporter;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import org.junit.jupiter.api.Test;

class SummaryLoggingHealthReporterTest {

    private static final NodeHealthReport REPORT_1 =
            new NodeHealthReport(NodeHealthStatus.HEALTHY, TestNodeRefs.REFS[0]);
    private static final NodeHealthReport REPORT_2 =
            new NodeHealthReport(NodeHealthStatus.UNHEALTHY, TestNodeRefs.REFS[1]);

    @Test
    void smokeTest() {
        SummaryLoggingHealthReporter reporter = new SummaryLoggingHealthReporter();

        assertThat("ToggleableReporter is disabled by default", reporter.isEnabled(), is(false));
        reporter.onStart();
        reporter.processReports(set(REPORT_1, REPORT_2), AssertHealthProcessorPlugin.class);
        reporter.onStop();

    }

}