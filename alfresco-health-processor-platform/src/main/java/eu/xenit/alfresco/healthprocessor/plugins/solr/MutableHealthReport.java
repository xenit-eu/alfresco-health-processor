package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.alfresco.service.cmr.repository.NodeRef;

@AllArgsConstructor
public class MutableHealthReport {

    @Getter
    private NodeHealthReport healthReport;

    public MutableHealthReport(NodeRef nodeRef) {
        this(new NodeHealthReport(null, nodeRef));
    }

    public MutableHealthReport(NodeHealthStatus healthStatus, NodeRef nodeRef, String... messages) {
        this(new NodeHealthReport(healthStatus, nodeRef, messages));
    }

    private void mark(NodeHealthStatus healthStatus, String reason) {
        Set<String> messages = new HashSet<>(healthReport.getMessages());
        messages.add(reason);
        NodeHealthReport copy = new NodeHealthReport(healthStatus, healthReport.getNodeRef(), messages);
        healthReport = copy;
    }

    public void markUnhealthy(String reason) {
        mark(NodeHealthStatus.UNHEALTHY, reason);
    }

    public void markUnknown(String reason) {
        if (healthReport.getStatus() == null) {
            mark(NodeHealthStatus.NONE, reason);
        } else {
            mark(healthReport.getStatus(), reason);
        }
    }

    public void markHealthy(String reason) {
        if (healthReport.getStatus() == null || healthReport.getStatus() == NodeHealthStatus.NONE) {
            mark(NodeHealthStatus.HEALTHY, reason);
        } else {
            mark(healthReport.getStatus(), reason);
        }
    }

}
