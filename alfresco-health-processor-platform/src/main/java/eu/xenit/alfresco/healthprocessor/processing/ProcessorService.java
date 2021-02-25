package eu.xenit.alfresco.healthprocessor.processing;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ParameterCheck;

@AllArgsConstructor
@Slf4j
public class ProcessorService {

    private final ProcessorConfiguration configuration;
    private final IndexingStrategy indexingStrategy;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final Set<HealthProcessorPlugin> plugins;
    private final Set<HealthReporter> reporters;

    public void execute() {
        indexingStrategy.reset();

        if (hasNoEnabledPlugins()) {
            log.warn("Health-Processor scheduled but not a single enabled plugin found.");
            return;
        }

        log.debug("Health-Processor: STARTING...");
        forEachEnabledReporter(HealthReporter::onStart);

        Set<NodeRef> nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        while (!nodesToProcess.isEmpty()) {
            this.processNodeBatch(nodesToProcess);
            nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        }

        forEachEnabledReporter(HealthReporter::onStop);
        log.debug("Health-Processor: DONE");
    }

    private void processNodeBatch(Set<NodeRef> nodesToProcess) {
        ParameterCheck.mandatory("nodesToProcess", nodesToProcess);

        for (HealthProcessorPlugin plugin : plugins) {
            retryingTransactionHelper.doInTransaction(() -> {
                Set<NodeRef> copy = new HashSet<>(nodesToProcess);
                this.processNodeBatchInTransaction(copy, plugin);
                return null;
            }, configuration.isReadOnly(), true);
        }
    }

    private void processNodeBatchInTransaction(Set<NodeRef> nodesToProcess, HealthProcessorPlugin plugin) {
        if (!plugin.isEnabled()) {
            log.debug("Plugin '{}' not enabled", plugin.getClass().getCanonicalName());
            return;
        }

        log.debug("Plugin '{}' will process #{} nodes", plugin.getClass().getCanonicalName(),
                nodesToProcess.size());

        Set<NodeHealthReport> reports = plugin.process(nodesToProcess);
        processReports(reports, plugin.getClass());
    }

    private void processReports(Set<NodeHealthReport> reports, Class<? extends HealthProcessorPlugin> pluginClass) {
        if (reports == null || reports.isEmpty()) {
            return;
        }

        forEachEnabledReporter(r -> {
            Set<NodeHealthReport> copy = new HashSet<>(reports);
            r.processReports(copy, pluginClass);
        });
    }

    private void forEachEnabledReporter(Consumer<HealthReporter> consumer) {
        if (reporters == null || reporters.isEmpty()) {
            return;
        }
        reporters.stream()
                .filter(HealthReporter::isEnabled)
                .forEach(consumer);
    }

    private boolean hasNoEnabledPlugins() {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        return plugins.stream().noneMatch(HealthProcessorPlugin::isEnabled);
    }
}
