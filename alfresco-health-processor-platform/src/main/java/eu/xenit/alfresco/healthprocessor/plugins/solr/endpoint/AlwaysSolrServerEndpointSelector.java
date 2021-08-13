package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Collections;
import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef.Status;

public class AlwaysSolrServerEndpointSelector implements SolrServerEndpointSelector {

    private final SearchEndpoint searchEndpoint;

    public AlwaysSolrServerEndpointSelector(String filter, SearchEndpoint searchEndpoint) {
        this.searchEndpoint = searchEndpoint;
    }

    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        return Collections.singleton(searchEndpoint);
    }
}
