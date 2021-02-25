package eu.xenit.alfresco.healthprocessor.processing;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;

@AllArgsConstructor
@Slf4j
public class ProcessorService {

    private final ProcessorConfiguration configuration;
    private final IndexingStrategy indexingStrategy;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final Set<HealthProcessorPlugin> plugins;

    public void execute() {
        indexingStrategy.reset();

        log.debug("Health-Processor: STARTING...");

        Set<NodeRef> nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        while (!nodesToProcess.isEmpty()) {
            this.processNodeBatch(nodesToProcess);
            nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        }

        log.debug("Health-Processor: DONE");
    }

    private void processNodeBatch(Set<NodeRef> nodesToProcess) {
        for (HealthProcessorPlugin plugin : plugins) {
            retryingTransactionHelper.doInTransaction(() -> {
                // TODO take copy of nodesToProcess to avoid side effects
                this.processNodeBatchInTransaction(nodesToProcess, plugin);
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
        plugin.process(nodesToProcess);
    }
}
