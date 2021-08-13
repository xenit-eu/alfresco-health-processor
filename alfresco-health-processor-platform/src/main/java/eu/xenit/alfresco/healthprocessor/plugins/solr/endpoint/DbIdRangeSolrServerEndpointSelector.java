package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef.Status;

@AllArgsConstructor
public class DbIdRangeSolrServerEndpointSelector implements SolrServerEndpointSelector {

    private final Long dbIdStart;
    private final Long dbIdEnd;
    private final SearchEndpoint endpoint;

    public DbIdRangeSolrServerEndpointSelector(String filter, SearchEndpoint endpoint) {
        this(filter.split("-", 2), endpoint);
    }

    private DbIdRangeSolrServerEndpointSelector(String[] filterParts, SearchEndpoint endpoint) {
        this(Long.parseUnsignedLong(
                        Objects.requireNonNull(filterParts[0], "Filter must be 2 numbers separated with a dash"), 10),
                Long.parseUnsignedLong(
                        Objects.requireNonNull(filterParts[1], "Filter must be 2 numbers separated with a dash"), 10),
                endpoint);
    }


    @Override
    public Set<SearchEndpoint> getSearchEndpointsForNode(Status nodeRef) {
        if (nodeRef.getDbId() >= dbIdStart && nodeRef.getDbId() < dbIdEnd) {
            return Collections.singleton(endpoint);
        }
        return Collections.emptySet();
    }
}
