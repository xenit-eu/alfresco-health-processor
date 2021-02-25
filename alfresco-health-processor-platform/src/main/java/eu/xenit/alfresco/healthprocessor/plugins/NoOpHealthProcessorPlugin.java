package eu.xenit.alfresco.healthprocessor.plugins;

import eu.xenit.alfresco.healthprocessor.plugins.api.SingleNodeHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
public class NoOpHealthProcessorPlugin extends SingleNodeHealthProcessorPlugin {

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected NodeHealthReport process(NodeRef nodeRef) {
        return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
    }
}
