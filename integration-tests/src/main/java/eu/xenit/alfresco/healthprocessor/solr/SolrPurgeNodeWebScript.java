package eu.xenit.alfresco.healthprocessor.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import eu.xenit.alfresco.healthprocessor.plugins.solr.JSONResponseHandler;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpointSelector;
import java.io.IOException;
import java.util.Properties;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

public class SolrPurgeNodeWebScript extends SolrNodeHandlerWebScript {

    public SolrPurgeNodeWebScript(NodeFinder nodeFinder,
                                  SearchEndpointSelector endpointSelector,
                                  Properties globalProperties) {
        super(nodeFinder, endpointSelector, globalProperties);
    }

    @Override
    protected BooleanNode handleNode(SearchEndpoint endpoint, NodeRef.Status nodeStatus) throws IOException {
        HttpUriRequest purgeRequest = new HttpGet(endpoint.getAdminUri().resolve(
                "cores?action=purge&nodeid=" + nodeStatus.getDbId() + "&wt=json&coreName=" + endpoint.getCoreName()));

        JsonNode node = httpClient.execute(purgeRequest, new JSONResponseHandler());
        return BooleanNode.valueOf(
                node.path("action").path(endpoint.getCoreName()).path("status").asText().equals("scheduled"));
    }
}
