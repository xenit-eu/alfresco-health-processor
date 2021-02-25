package eu.xenit.alfresco.healthprocessor.plugins.api;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
public abstract class SingleNodeHealthProcessorPlugin extends AbstractHealthProcessorPlugin {

    @Override
    protected Logger getLogger() {
        return log;
    }

    public void doProcess(Set<NodeRef> nodeRefs) {
        for (NodeRef nodeRef : nodeRefs) {
            getLogger().trace("Processing NodeRef: {}", nodeRef);
            process(nodeRef);
            getLogger().trace("Done processing NodeRef: {}", nodeRef);
        }
    }

    protected abstract void process(NodeRef nodeRef);
}
