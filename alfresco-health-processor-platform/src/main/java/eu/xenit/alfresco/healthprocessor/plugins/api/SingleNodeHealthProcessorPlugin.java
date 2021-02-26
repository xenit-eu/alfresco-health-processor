package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
public abstract class SingleNodeHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    @Override
    protected Logger getLogger() {
        return log;
    }

    public Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        Set<NodeHealthReport> ret = new HashSet<>();
        for (NodeRef nodeRef : nodeRefs) {
            getLogger().trace("Processing NodeRef: {}", nodeRef);
            NodeHealthReport report = process(nodeRef);
            if (report != null) {
                ret.add(report);
            }
            getLogger().trace("Done processing NodeRef: {}", nodeRef);
        }
        return ret;
    }

    protected abstract NodeHealthReport process(NodeRef nodeRef);
}
