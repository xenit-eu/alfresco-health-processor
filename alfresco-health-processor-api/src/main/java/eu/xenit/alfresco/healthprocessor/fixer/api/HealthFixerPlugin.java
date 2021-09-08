package eu.xenit.alfresco.healthprocessor.fixer.api;

import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Extension point for plugging in logic into the Health-Processor to fix unhealthy nodes.
 */
public interface HealthFixerPlugin extends BaseExtension {

    /**
     * Process a batch of unhealthy node reports. Each execution will be wrapped in a new transaction by the Health-Processor.
     *
     * @param pluginClass      the health processor plugin that reported a problem
     * @param unhealthyReports the batch of {@link NodeHealthReport}s of unhealthy nodes to fix
     * @return Revised health reports after unhealthy nodes have been fixed.
     */
    @Nonnull
    Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> unhealthyReports);
}
