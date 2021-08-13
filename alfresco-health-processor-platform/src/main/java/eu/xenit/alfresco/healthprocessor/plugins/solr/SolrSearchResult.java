package eu.xenit.alfresco.healthprocessor.plugins.solr;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;

@Data
public class SolrSearchResult {

    private final Set<NodeRef.Status> found = new HashSet<>();
    private final Set<NodeRef.Status> missing = new HashSet<>();
    private final Set<NodeRef.Status> notIndexed = new HashSet<>();
}
