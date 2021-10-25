package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

/**
 * {@link HealthFixerPlugin} that already has an <code>enabled</code> property
 */
@Slf4j
public abstract class ToggleableHealthProcessorPlugin implements HealthProcessorPlugin {

    @Getter
    @Setter
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
