package eu.xenit.alfresco.healthprocessor.elastic;

import eu.xenit.alfresco.healthprocessor.fixer.elastic.ElasticMissingOrOutdatedNodeFixerPlugin;
import java.io.IOException;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ElasticConfigureIndexNodeFixerPlugin extends AbstractWebScript {

    private final ElasticMissingOrOutdatedNodeFixerPlugin fixerPlugin;

    public ElasticConfigureIndexNodeFixerPlugin(
            ElasticMissingOrOutdatedNodeFixerPlugin fixerPlugin) {
        this.fixerPlugin = fixerPlugin;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        fixerPlugin.setEnabled(Boolean.parseBoolean(req.getParameter("enabled")));
    }
}
