package eu.xenit.alfresco.healthprocessor.elastic;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor.ActionResponse;
import eu.xenit.alfresco.healthprocessor.index.NodeFinder;

public class ElasticPurgeNodeWebScript extends AbstractWebScript
{

    private final NodeFinder nodeFinder;

    private final SearchEndpointSelector<SearchEndpoint> endpointSelector;

    private final ElasticRequestExecutor elasticRequestExecutor;

    public ElasticPurgeNodeWebScript(NodeFinder nodeFinder, SearchEndpointSelector<SearchEndpoint> endpointSelector,
            ElasticRequestExecutor elasticRequestExecutor)
    {
        this.nodeFinder = nodeFinder;
        this.endpointSelector = endpointSelector;
        this.elasticRequestExecutor = elasticRequestExecutor;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        List<NodeRef.Status> nodesToPurge = nodeFinder.findNodes();

        Map<NodeRef, List<BooleanNode>> results = new HashMap<>();
        for (NodeRef.Status nodeRef : nodesToPurge)
        {
            Set<SearchEndpoint> endpoints = endpointSelector.getSearchEndpointsForNode(nodeRef);

            for (SearchEndpoint endpoint : endpoints)
            {
                try
                {
                    BooleanNode result = handleNode(endpoint, nodeRef);
                    results.computeIfAbsent(nodeRef.getNodeRef(), nR -> new ArrayList<>()).add(result);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        objectMapper.writeTree(jsonFactory.createGenerator(res.getWriter()), toJson(results));
    }

    private BooleanNode handleNode(SearchEndpoint endpoint, NodeRef.Status nodeStatus) throws IOException
    {
        ActionResponse actionResponse = elasticRequestExecutor.deleteNodeIndexEntry(endpoint, nodeStatus.getNodeRef().getId());
        return BooleanNode.valueOf(actionResponse.isSuccessFull());
    }

    private JsonNode toJson(Map<NodeRef, ? extends List<BooleanNode>> results)
    {
        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        ObjectNode objectNode = nodeFactory.objectNode();

        results.forEach((nodeRef, resultList) -> {
            BooleanNode node = null;
            for (BooleanNode bNode : resultList)
            {
                if (node == null || !node.isBoolean())
                {
                    node = bNode;
                }
            }
            objectNode.set(nodeRef.toString(), node);
        });
        return objectNode;
    }
}
