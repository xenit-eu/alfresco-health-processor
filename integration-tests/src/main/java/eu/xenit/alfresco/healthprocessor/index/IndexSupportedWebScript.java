package eu.xenit.alfresco.healthprocessor.index;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Set;

import org.alfresco.util.ResourceFinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class IndexSupportedWebScript extends AbstractWebScript implements ApplicationContextAware
{

    private final Set<String> potentialIndexSubsystemNames;

    private ApplicationContext context;

    public IndexSupportedWebScript(Set<String> potentialIndexSubsystemNames)
    {
        this.potentialIndexSubsystemNames = potentialIndexSubsystemNames;
    }

    public void setApplicationContext(ApplicationContext context)
    {
        this.context = context;
    }

    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

        ObjectNode result = jsonNodeFactory.objectNode();
        ResourceFinder resourceFinder = new ResourceFinder(context);
        for (String subsystemName : potentialIndexSubsystemNames)
        {
            String contextFilePattern = "classpath*:alfresco/subsystems/Search/" + subsystemName + "/*-context.xml";
            boolean supported = resourceFinder.getResources(contextFilePattern).length > 0;
            result.put(subsystemName, supported);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        objectMapper.writeTree(jsonFactory.createGenerator(res.getWriter()), result);
    }
}
