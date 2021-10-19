package eu.xenit.alfresco.healthprocessor.reporter.store;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NodeHealthReportClassifier {

    private static final Set<NodeHealthStatus> INTERESTING_STATUSES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(NodeHealthStatus.UNREPORTED, NodeHealthStatus.UNHEALTHY, NodeHealthStatus.FIXED)));

    public boolean shouldBeStored(NodeHealthReport healthReport) {
        return INTERESTING_STATUSES.contains(healthReport.getStatus());
    }

}
