package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Set;
import javax.annotation.Nonnull;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Main extension point for plugging in (custom) logic into the Health-Processor. All implementations available in the
 * Spring context (of the Health-Processor subsystem) will be registered and, {@link #isEnabled() if enabled}, triggered
 * by the Health-Processor.
 *
 * @see ToggleableHealthProcessorPlugin abstract implementation that can easily be enabled / disabled via a property
 * @see SingleNodeHealthProcessorPlugin implementation dividing the batch in individual {@link
 * SingleNodeHealthProcessorPlugin#process(NodeRef)} calls.
 */
public interface HealthProcessorPlugin {

    boolean isEnabled();

    /**
     * Process a batch of nodes. Each execution will be wrapped in a new transaction by the Health-Processor.
     *
     * @param nodeRefs the batch of {@link NodeRef}s to process
     * @return is is not mandatory to return anything. All returned {@link NodeHealthReport reports} will be offered to
     * {@link eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter} instances.
     */
    @Nonnull
    Set<NodeHealthReport> process(Set<NodeRef> nodeRefs);

}
