package eu.xenit.alfresco.healthprocessor.solr;

import eu.xenit.alfresco.healthprocessor.fixer.solr.SolrMissingNodeFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.solr.SolrMissingNodeFixerPluginImpl;
import java.io.IOException;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class SolrConfigureIndexNodeFixerPlugin extends AbstractWebScript {

    private final SolrMissingNodeFixerPlugin fixerPlugin;

    public SolrConfigureIndexNodeFixerPlugin(
            SolrMissingNodeFixerPlugin fixerPlugin) {
        this.fixerPlugin = fixerPlugin;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        fixerPlugin.setEnabled(Boolean.parseBoolean(req.getParameter("enabled")));
    }
}
