package eu.xenit.alfresco.healthprocessor.solr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.List;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class SolrCheckNodeWebScript extends AbstractWebScript {

    private final NodeFinder nodeFinder;

    public SolrCheckNodeWebScript(NodeFinder nodeFinder) {
        this.nodeFinder = nodeFinder;
    }

    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        List<NodeRef> nodes = nodeFinder.findWithSolr();

        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

        ArrayNode results = jsonNodeFactory.arrayNode(nodes.size());
        for (NodeRef node : nodes) {
            results.add(node.toString());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        objectMapper.writeTree(jsonFactory.createGenerator(res.getWriter()), results);
    }
}
