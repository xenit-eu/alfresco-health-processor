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
    private NodeHealthStatus forcedHealthStatus = null;

    private Set<String> messages = new HashSet<>();

    public MutableHealthReport(@NonNull NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public MutableHealthReport(@NonNull NodeHealthStatus healthStatus, @Nonnull NodeRef nodeRef, String... messages) {
        this.healthStatus = healthStatus;
        this.nodeRef = nodeRef;
        this.messages = new HashSet<>(Arrays.asList(messages));
    }

    private void mark(NodeHealthStatus healthStatus, String reason, boolean forced) {
        this.healthStatus = healthStatus;
        if (forced) {
            forcedHealthStatus = healthStatus;
        }
        messages.add(reason);
    }

    public void markUnhealthy(String reason) {
        mark(NodeHealthStatus.UNHEALTHY, reason, true);
    }

    public void markUnknownForced(String reason) {
        mark(NodeHealthStatus.NONE, reason, true);
    }

    public void markUnknown(String reason) {
        if (healthStatus == null) {
            mark(NodeHealthStatus.NONE, reason, false);
        } else {
            mark(healthStatus, reason, false);
        }
    }

    public void markHealthy(String reason) {
        if (healthStatus == null || healthStatus == NodeHealthStatus.NONE) {
            mark(NodeHealthStatus.HEALTHY, reason, false);
        } else {
            mark(healthStatus, reason, false);
        }
    }

    public NodeHealthReport getHealthReport() {
        Objects.requireNonNull(healthStatus, "healthStatus");
        if (forcedHealthStatus != null) {
            return new NodeHealthReport(forcedHealthStatus, nodeRef, messages);
        }
        return new NodeHealthReport(healthStatus, nodeRef, messages);
    }
}
