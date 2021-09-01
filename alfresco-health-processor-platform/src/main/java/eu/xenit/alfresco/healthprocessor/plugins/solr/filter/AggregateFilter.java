package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * Aggregated filter.
 * <p>
 * If any of the sub-filters indicate that the node should be ignored, the node will be ignored
 */
@AllArgsConstructor
@ToString
public class AggregateFilter implements SolrNodeFilter {

    private final List<SolrNodeFilter> filters;

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        for (SolrNodeFilter filter : filters) {
            if (filter.isIgnored(nodeRefStatus)) {
                return true;
            }
        }
        return false;
    }
}
