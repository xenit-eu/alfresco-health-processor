package eu.xenit.alfresco.healthprocessor.example;

import java.util.HashMap;
import java.util.Map;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ExampleExtensionsGetInfoWebScript extends DeclarativeWebScript {

    private final ExampleHealthProcessorPlugin plugin;

    public ExampleExtensionsGetInfoWebScript(ExampleHealthProcessorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> ret = new HashMap<>();

        ret.put("numberOfNodesProcessed", plugin.getNumberOfNodesProcessed());

        return ret;
    }
}
