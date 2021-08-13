package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef;

public interface SolrServerEndpointSelector {

    Set<SearchEndpoint> getSearchEndpointsForNode(NodeRef.Status nodeRef);
}
