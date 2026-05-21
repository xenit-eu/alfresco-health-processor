package eu.xenit.alfresco.healthprocessor.solr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.encryption.AlfrescoKeyStore;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.client.HttpClient;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.index.NodeFinder;

public abstract class SolrNodeHandlerWebScript extends AbstractWebScript {

    private final NodeFinder nodeFinder;

    private final SearchEndpointSelector<SolrEndpoint> endpointSelector;

    protected final SolrRequestExecutor solrRequestExecutor;

    protected SolrNodeHandlerWebScript(NodeFinder nodeFinder,
                                       SearchEndpointSelector<SolrEndpoint> endpointSelector,
                                       SolrRequestExecutor solrRequestExecutor) {
        this.nodeFinder = nodeFinder;
        this.endpointSelector = endpointSelector;
        this.solrRequestExecutor = solrRequestExecutor;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        List<NodeRef.Status> nodesToPurge = nodeFinder.findNodes();

        Map<NodeRef, List<JsonNode>> results = new HashMap<>();
        for (NodeRef.Status nodeRef : nodesToPurge) {
            Set<SolrEndpoint> endpoints = endpointSelector.getSearchEndpointsForNode(nodeRef);

            for (SolrEndpoint endpoint : endpoints) {
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

    protected abstract JsonNode handleNode(SolrEndpoint endpoint, NodeRef.Status nodeStatus) throws IOException;

}
