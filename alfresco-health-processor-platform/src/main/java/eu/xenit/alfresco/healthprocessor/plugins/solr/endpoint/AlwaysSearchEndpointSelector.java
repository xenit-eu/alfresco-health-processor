package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Collections;
import java.util.Set;
import lombok.ToString;
import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * Simple endpoint selector that selects a single endpoint for all nodes
 */
@ToString
public class AlwaysSearchEndpointSelector implements SearchEndpointSelector {

    private final SearchEndpoint searchEndpoint;

    public AlwaysSearchEndpointSelector(String filter, SearchEndpoint searchEndpoint) {
        this.searchEndpoint = searchEndpoint;
    }

    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        return Collections.singleton(searchEndpoint);
    }
}
