package eu.xenit.alfresco.healthprocessor.plugins.solr;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Search result from a solr search operation
 */
@Value
@AllArgsConstructor
public class SolrSearchResult {

    public SolrSearchResult() {
        this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * Nodes that have been found by the search operation
     */
    Set<NodeRef.Status> found;
    /**
     * Nodes that were requested, but not found by the search operation
     */
    Set<NodeRef.Status> missing;
    /**
     * Nodes that, according to their transaction number, are not yet indexed when the search operation was executed
     */
    Set<NodeRef.Status> notIndexed;
    /**
     * Nodes that are returned by the search operation multiple times
     */
    Set<NodeRef.Status> duplicate;
}
