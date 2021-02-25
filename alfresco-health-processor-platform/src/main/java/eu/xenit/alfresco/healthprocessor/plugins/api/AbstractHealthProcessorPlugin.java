package eu.xenit.alfresco.healthprocessor.plugins.api;

import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
@Data
public abstract class AbstractHealthProcessorPlugin implements HealthProcessorPlugin {

    private boolean enabled;

    protected Logger getLogger() {
        return log;
    }

    @Override
    public final void process(Set<NodeRef> nodeRefs) {
        getLogger().debug("Processing batch of #{} nodeRefs", nodeRefs.size());
        getLogger().trace("[{}]", nodeRefs);
        doProcess(nodeRefs);
    }

    protected abstract void doProcess(Set<NodeRef> nodeRefs);
}
