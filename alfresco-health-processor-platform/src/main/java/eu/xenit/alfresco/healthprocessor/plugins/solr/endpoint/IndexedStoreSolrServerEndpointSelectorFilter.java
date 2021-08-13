package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;

@Slf4j
@AllArgsConstructor
public class IndexedStoreSolrServerEndpointSelectorFilter implements SolrServerEndpointSelector {

    private SolrServerEndpointSelector endpointSelector;

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
