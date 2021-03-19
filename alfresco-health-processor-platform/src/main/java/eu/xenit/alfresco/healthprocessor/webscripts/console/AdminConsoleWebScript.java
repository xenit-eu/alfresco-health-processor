package eu.xenit.alfresco.healthprocessor.webscripts.console;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

@EqualsAndHashCode(callSuper = true)
@Value
public class AdminConsoleWebScript extends DeclarativeWebScript {

    ResponseViewRenderer viewRenderer;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> model = new HashMap<>();

        model.put("healthprocessor", viewRenderer.renderView());

        return model;
    }

}
