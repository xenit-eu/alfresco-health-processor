package eu.xenit.alfresco.healthprocessor.plugins;

import eu.xenit.alfresco.healthprocessor.plugins.api.AbstractHealthProcessorPlugin;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
public class LoggingHealthProcessorPlugin extends AbstractHealthProcessorPlugin {

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected void doProcess(Set<NodeRef> nodeRefs) {
        log.info("Processing #{} NodeRefs", nodeRefs.size());
        log.trace("[{}]", nodeRefs);
    }
}
