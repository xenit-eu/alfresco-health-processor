package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;

public class TestReports {

    private TestReports() {
        // private ctor to hide implicit public one
    }

    private static int testRefsPointer = 0;

    public static NodeHealthReport unhealthy() {
        return random(NodeHealthStatus.UNHEALTHY);
    }

    public static NodeHealthReport healthy() {
        return random(NodeHealthStatus.HEALTHY);
    }

    public static NodeHealthReport random(NodeHealthStatus status) {
        return new NodeHealthReport(status, TestNodeRefs.REFS[testRefsPointer++]);
    }

}
