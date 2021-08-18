package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Selects which search endpoints should be queried check if a node is indexed there.
 */
public interface SearchEndpointSelector {

    /**
     * Retrieves the set of search endpoints which should have the node indexed (eventually).
     * <p>
     * This set can also be empty if the node is not indexed anywhere.
     * Search endpoints that have not yet indexed the node because they are behind on transactions should
     * be returned as well.
     * Search endpoints that will never index the node because the node is not part of the shard should not be returned.
     *
     * @param nodeRef The node to select search endpoints for
     * @return
     */
    Set<SearchEndpoint> getSearchEndpointsForNode(NodeRef.Status nodeRef);
}
