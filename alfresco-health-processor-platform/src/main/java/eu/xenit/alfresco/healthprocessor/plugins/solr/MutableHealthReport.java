package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Health report that is initially unset and can be marked as healthy, unhealthy or unknown for a reason
 */
public class MutableHealthReport {

    private final NodeRef nodeRef;

    @Nullable
    private NodeHealthStatus healthStatus = null;
    private final Set<NodeIndexHealthReport> endpointHealthReports = new HashSet<>();

    private Set<String> messages = new HashSet<>();

    public MutableHealthReport(@NonNull NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public MutableHealthReport(@NonNull NodeHealthStatus healthStatus, @Nonnull NodeRef nodeRef, String... messages) {
        this.healthStatus = healthStatus;
        this.nodeRef = nodeRef;
        this.messages = new HashSet<>(Arrays.asList(messages));
    }

    public void addHealthReport(IndexHealthStatus healthStatus, NodeRef.Status nodeRefStatus,
            SearchEndpoint searchEndpoint) {
        addHealthReport(new NodeIndexHealthReport(healthStatus, nodeRefStatus, searchEndpoint));
    }

    public void addHealthReport(NodeIndexHealthReport endpointHealthReport) {
        if (healthStatus != null) {
            throw new IllegalStateException(
                    "Can not add endpoint health reports when a health status is set directly.");
        }
        endpointHealthReports.add(endpointHealthReport);
    }

    private NodeHealthStatus getHealthStatus() {
        if (healthStatus != null) {
            return healthStatus;
        }

        IndexHealthStatus highestHealthStatus = IndexHealthStatus.UNSET;
        for (NodeIndexHealthReport endpointHealthReport : endpointHealthReports) {
            IndexHealthStatus healthStatus = endpointHealthReport.getHealthStatus();
            if (healthStatus.ordinal() < highestHealthStatus.ordinal()) {
                highestHealthStatus = healthStatus;
            }
        }

        if (highestHealthStatus == IndexHealthStatus.UNSET) {
            throw new IllegalStateException("Can not have no health status set and have no endpoint health reports");
        }
        return highestHealthStatus.getNodeHealthStatus();
    }

    public NodeHealthReport getHealthReport() {
        Set<String> allMessages = Stream.concat(
                messages.stream(),
                endpointHealthReports.stream().map(NodeIndexHealthReport::getMessage)
        ).collect(Collectors.toSet());

        NodeHealthReport nodeHealthReport = new NodeHealthReport(getHealthStatus(), nodeRef, allMessages);
        nodeHealthReport.data(NodeIndexHealthReport.class).addAll(endpointHealthReports);
        return nodeHealthReport;
    }
}
