package eu.xenit.alfresco.healthprocessor.checker.solr.filter;

import java.util.Collections;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef.Status;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.filter.NodeFilter;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Wrapping {@link SearchEndpointSelector} that applies a {@link NodeFilter} to ignore all nodes that are ignored by a filter
 */
@Slf4j
@AllArgsConstructor
@ToString
public class FilteringSearchEndpointSelector implements SearchEndpointSelector<SearchEndpoint> {

    private final SearchEndpointSelector<SearchEndpoint> searchEndpointSelector;
    private final NodeFilter filter;


    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        if (filter.isIgnored(nodeRef)) {
            log.trace("Node {} is ignored by a filter.", nodeRef.getNodeRef());

            return Collections.emptySet();
        }
        return searchEndpointSelector.getSearchEndpointsForNode(nodeRef);
    }
}
