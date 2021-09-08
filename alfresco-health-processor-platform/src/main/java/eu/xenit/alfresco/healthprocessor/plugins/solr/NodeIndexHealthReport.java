package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
public class NodeIndexHealthReport {

    IndexHealthStatus healthStatus;
    NodeRef.Status nodeRefStatus;
    SearchEndpoint endpoint;

    public String getMessage() {
        return healthStatus.formatReason(endpoint);
    }


    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum IndexHealthStatus {
        // Fields are ordered by priority they take when multiple are present and the NodeHealthStatus has to be resolved from them
        // Higher in this list = higher priority
        EXCEPTION(NodeHealthStatus.NONE, "Exception occurred while checking node in search index %s."),
        NOT_FOUND(NodeHealthStatus.UNHEALTHY, "Node is missing in search index %s."),
        DUPLICATE(NodeHealthStatus.UNHEALTHY, "Node is present multiple times in search index %s."),
        FOUND(NodeHealthStatus.HEALTHY, "Node is present in search index %s."),
        NOT_INDEXED(NodeHealthStatus.NONE, "Node is not yet indexed in search index %s (TX not yet processed).");

        @Getter
        NodeHealthStatus nodeHealthStatus;
        String formatMessage;

        String formatReason(SearchEndpoint endpoint) {
            return String.format(formatMessage, endpoint);
        }
    }
}
