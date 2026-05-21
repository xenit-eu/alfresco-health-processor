package eu.xenit.alfresco.healthprocessor.checker;

import org.alfresco.service.cmr.repository.NodeRef;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
public class NodeIndexHealthReport<T extends SearchEndpoint> {

    IndexHealthStatus healthStatus;
    NodeRef.Status nodeRefStatus;
    T endpoint;

    public String getMessage() {
        return healthStatus.formatReason(endpoint);
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum IndexHealthStatus {
        // Fields are ordered by priority they take when multiple are present and the NodeHealthStatus has to be resolved from them
        // Higher in this list = higher priority
        EXCEPTION(NodeHealthStatus.NONE, "Exception occurred while checking node in search index %s."),
        NOT_INDEX_RELEVANT(NodeHealthStatus.HEALTHY, "Node is not found in search index %s and is not relevant for indexation in it."),
        NOT_FOUND(NodeHealthStatus.UNHEALTHY, "Node is missing in search index %s."),
        DUPLICATE(NodeHealthStatus.UNHEALTHY, "Node is present multiple times in search index %s."),
        FOUND_UNDELETED(NodeHealthStatus.UNHEALTHY, "Node is present in search index %s but should not have been."),
        FOUND_OUTDATED(NodeHealthStatus.UNHEALTHY, "Node is present in search index %s but is outdated."),
        FOUND_PATH_MISSING(NodeHealthStatus.UNHEALTHY, "Node is present in search index %s but lacks a PATH field."),
        FOUND(NodeHealthStatus.HEALTHY, "Node is present in search index %s."),
        NOT_INDEXED(NodeHealthStatus.NONE, "Node is not yet indexed in search index %s (TX not yet processed).");

        @Getter
        NodeHealthStatus nodeHealthStatus;
        String formatMessage;

        public String formatReason(SearchEndpoint endpoint) {
            return String.format(formatMessage, endpoint);
        }
    }
}
