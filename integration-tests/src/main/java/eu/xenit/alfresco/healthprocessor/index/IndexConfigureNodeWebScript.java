package eu.xenit.alfresco.healthprocessor.index;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class IndexConfigureNodeWebScript extends AbstractWebScript
{

    private final SwitchableApplicationContextFactory searchSubsystem;

    public IndexConfigureNodeWebScript(SwitchableApplicationContextFactory searchSubsystem)
    {
        this.searchSubsystem = searchSubsystem;
    }

    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        String subsystem = req.getParameter("subsystem");
        if (subsystem != null && !subsystem.isBlank())
        {
            // constant SOURCE_BEAN_PROPERTY in SwitchableApplicationContextFactory is not accessible
            searchSubsystem.setProperty("sourceBeanName", subsystem);
        }

        JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

        ObjectNode result = jsonNodeFactory.objectNode();
        result.put("activeSubsystem", this.searchSubsystem.getCurrentSourceBeanName());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        objectMapper.writeTree(jsonFactory.createGenerator(res.getWriter()), result);
    }
}
