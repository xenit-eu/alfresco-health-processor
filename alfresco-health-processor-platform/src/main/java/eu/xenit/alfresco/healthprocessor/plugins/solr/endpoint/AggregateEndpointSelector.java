package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef.Status;


@AllArgsConstructor
public class AggregateEndpointSelector implements SolrServerEndpointSelector {

    private final Set<SolrServerEndpointSelector> endpointSelectors;

    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        return endpointSelectors.stream()
                .flatMap(selector -> selector.getSearchEndpointsForNode(nodeRef).stream())
                .collect(Collectors.toSet());
    }
}
