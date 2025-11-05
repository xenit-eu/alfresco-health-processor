package eu.xenit.alfresco.healthprocessor.filter;

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
public class AggregateFilter implements NodeFilter {

    private final List<NodeFilter> filters;

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        for (NodeFilter filter : filters) {
            if (filter.isIgnored(nodeRefStatus)) {
                return true;
            }
        }
        return false;
    }
}
