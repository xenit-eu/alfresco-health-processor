package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * Wrapper for an {@link SearchEndpointSelector} that filters endpoints down to only those that match the store that is indexed by the {@link SearchEndpoint}
 *
 * Applying this filter to the end of a chain ensures that only endpoints that index the store that the node lives in are returned.
 */
@Slf4j
@AllArgsConstructor
public class IndexedStoreFilterSearchEndpointSelector implements SearchEndpointSelector {

    private SearchEndpointSelector endpointSelector;

    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        Set<SearchEndpoint> endpoints = endpointSelector.getSearchEndpointsForNode(nodeRef);

        return endpoints.stream()
                .filter(endpoint -> {
                    String store = nodeRef.getNodeRef().getStoreRef().toString();
                    if(store.equals(endpoint.getIndexedStore())) {
                        return true;
                    }
                    log.trace("Removing endpoint {} for node {} because stores don't match.", endpoint, nodeRef.getNodeRef());

                    return false;
                })
                .collect(Collectors.toSet());
    }
}
