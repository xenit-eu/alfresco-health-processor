package eu.xenit.alfresco.healthprocessor.plugins.solr;

import java.util.HashSet;
import java.util.Set;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Search result from a solr search operation
 */
@Value
public class SolrSearchResult {

    public SolrSearchResult() {
        found = new HashSet<>();
        missing = new HashSet<>();
        notIndexed = new HashSet<>();
    }

    public SolrSearchResult(Set<NodeRef.Status> found, Set<NodeRef.Status> missing, Set<NodeRef.Status> notIndexed) {
        this.found = found;
        this.missing = missing;
        this.notIndexed = notIndexed;
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

}
