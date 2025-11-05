package eu.xenit.alfresco.healthprocessor.endpoint;

import java.util.Collections;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef.Status;

import lombok.ToString;

/**
 * Simple endpoint selector that selects a single endpoint for all nodes
 */
@ToString
public class AlwaysSearchEndpointSelector<T extends SearchEndpoint> implements SearchEndpointSelector<T> {

    private final T searchEndpoint;

    public AlwaysSearchEndpointSelector(String filter, T searchEndpoint) {
        this.searchEndpoint = searchEndpoint;
    }

    @Override
    public Set<T> getSearchEndpointsForNode(Status nodeRef) {
        return Collections.singleton(searchEndpoint);
    }
}
