package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpointSelector;
import java.util.Collections;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * Wrapping {@link SearchEndpointSelector} that applies a {@link SolrNodeFilter} to ignore all nodes that are ignored by a filter
 */
@Slf4j
@AllArgsConstructor
public class FilteringSearchEndpointSelector implements SearchEndpointSelector {

    private final SearchEndpointSelector solrServerEndpointSelector;
    private final SolrNodeFilter filter;


    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        if (filter.isIgnored(nodeRef)) {
            log.trace("Node {} is ignored by a filter.", nodeRef.getNodeRef());

            return Collections.emptySet();
        }
        return solrServerEndpointSelector.getSearchEndpointsForNode(nodeRef);
    }
}
