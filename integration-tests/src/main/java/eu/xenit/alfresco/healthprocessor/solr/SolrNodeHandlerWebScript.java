package eu.xenit.alfresco.healthprocessor.solr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpointSelector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.xenit.alfresco.healthprocessor.plugins.solr.utils.HealthProcessorSimpleHttpClientFactory;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public abstract class SolrNodeHandlerWebScript extends AbstractWebScript {

    private final NodeFinder nodeFinder;

    private final SearchEndpointSelector endpointSelector;

    protected final HttpClient httpClient;

    protected SolrNodeHandlerWebScript(NodeFinder nodeFinder,
            SearchEndpointSelector endpointSelector,
                                       HealthProcessorSimpleHttpClientFactory clientFactory) {
        this.nodeFinder = nodeFinder;
        this.endpointSelector = endpointSelector;

         httpClient = clientFactory.createHttpClient();
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        List<NodeRef.Status> nodesToPurge = nodeFinder.findNodes();

        Map<NodeRef, List<JsonNode>> results = new HashMap<>();
        for (NodeRef.Status nodeRef : nodesToPurge) {
            Set<SearchEndpoint> endpoints = endpointSelector.getSearchEndpointsForNode(nodeRef);

            for (SearchEndpoint endpoint : endpoints) {
                try {
                    JsonNode result = handleNode(endpoint, nodeRef);
                    results.computeIfAbsent(nodeRef.getNodeRef(), nR -> new ArrayList<>()).add(result);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        objectMapper.writeTree(jsonFactory.createGenerator(res.getWriter()), toJson(results));
    }

    private JsonNode toJson(Map<NodeRef, ? extends List<JsonNode>> results) {
        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        ObjectNode objectNode = nodeFactory.objectNode();

        results.forEach((nodeRef, resultList) -> {
            ArrayNode resultArray = nodeFactory.arrayNode(resultList.size());
            resultArray.addAll(resultArray);
            objectNode.set(nodeRef.toString(), resultArray);
        });
        return objectNode;
    }

    protected abstract JsonNode handleNode(SearchEndpoint endpoint, NodeRef.Status nodeStatus) throws IOException;

}
