package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.alfresco.service.cmr.repository.NodeRef.Status;


/**
 * Collects {@link SearchEndpoint}s for a node from multiple sources
 */
@AllArgsConstructor
@ToString
public class AggregateSearchEndpointSelector implements SearchEndpointSelector {

    private final Set<SearchEndpointSelector> endpointSelectors;

    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        return endpointSelectors.stream()
                .flatMap(selector -> selector.getSearchEndpointsForNode(nodeRef).stream())
                .collect(Collectors.toSet());
    }
}
