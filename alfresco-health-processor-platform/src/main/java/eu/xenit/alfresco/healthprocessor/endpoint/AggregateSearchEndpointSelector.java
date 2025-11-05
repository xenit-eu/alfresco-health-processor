package eu.xenit.alfresco.healthprocessor.endpoint;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.NodeRef.Status;

import lombok.AllArgsConstructor;
import lombok.ToString;


/**
 * Collects {@link SearchEndpoint}s for a node from multiple sources
 */
@AllArgsConstructor
@ToString
public class AggregateSearchEndpointSelector<T extends SearchEndpoint> implements SearchEndpointSelector<T> {

    private final List<SearchEndpointSelector<T>> endpointSelectors;

    @Override
    public Set<T> getSearchEndpointsForNode(Status nodeRef) {
        return endpointSelectors.stream()
                .flatMap(selector -> selector.getSearchEndpointsForNode(nodeRef).stream())
                .collect(Collectors.toSet());
    }
}
