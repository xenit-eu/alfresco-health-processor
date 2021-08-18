package eu.xenit.alfresco.healthprocessor.plugins.solr;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Search result from a solr search operation
 */
@Value
public class SolrSearchResult {

    /**
     * Nodes that have been found by the search operation
     */
    Set<NodeRef.Status> found = new HashSet<>();
    /**
     * Nodes that were requested, but not found by the search operation
     */
    Set<NodeRef.Status> missing = new HashSet<>();
    /**
     * Nodes that, according to their transaction number, are not yet indexed when the search operation was executed
     */
    Set<NodeRef.Status> notIndexed = new HashSet<>();
}
