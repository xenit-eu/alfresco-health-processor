package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

    private Set<String> messages = new HashSet<>();

    public MutableHealthReport(@NonNull NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public MutableHealthReport(@NonNull NodeHealthStatus healthStatus, @Nonnull NodeRef nodeRef, String... messages) {
        this.healthStatus = healthStatus;
        this.nodeRef = nodeRef;
        this.messages = new HashSet<>(Arrays.asList(messages));
    }

    private void mark(NodeHealthStatus healthStatus, String reason) {
        this.healthStatus = healthStatus;
        messages.add(reason);
    }

    public void markUnhealthy(String reason) {
        mark(NodeHealthStatus.UNHEALTHY, reason);
    }

    public void markUnknown(String reason) {
        if (healthStatus == null) {
            mark(NodeHealthStatus.NONE, reason);
        } else {
            mark(healthStatus, reason);
        }
    }

    public void markHealthy(String reason) {
        if (healthStatus == null || healthStatus == NodeHealthStatus.NONE) {
            mark(NodeHealthStatus.HEALTHY, reason);
        } else {
            mark(healthStatus, reason);
        }
    }

    public NodeHealthReport getHealthReport() {
        Objects.requireNonNull(healthStatus, "healthStatus");
        return new NodeHealthReport(healthStatus, nodeRef, messages);
    }
}
