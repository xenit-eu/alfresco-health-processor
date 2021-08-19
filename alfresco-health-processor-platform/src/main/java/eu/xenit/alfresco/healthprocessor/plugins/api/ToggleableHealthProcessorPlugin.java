package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
@Data
public abstract class ToggleableHealthProcessorPlugin implements HealthProcessorPlugin {

    private boolean enabled;

    protected Logger getLogger() {
        return log;
    }

    @Nonnull
    @Override
    public final Set<NodeHealthReport> process(Set<NodeRef> nodeRefs) {
        getLogger().debug("Processing batch of #{} nodeRefs", nodeRefs.size());
        getLogger().trace("[{}]", nodeRefs);
        return doProcess(nodeRefs);
    }

    @Nonnull
    protected abstract Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs);
}
