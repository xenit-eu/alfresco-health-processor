package eu.xenit.alfresco.processor.processing;

import eu.xenit.alfresco.processor.indexing.IndexingStrategy;
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
        retryingTransactionHelper.doInTransaction(() -> {
            this.processNodeBatchInTransaction(nodesToProcess);
            return null;
        }, configuration.isReadOnly(), true);
    }

    private void processNodeBatchInTransaction(Set<NodeRef> nodesToProcess) {
        log.debug("Processing #{} nodes.", nodesToProcess.size());
    }
}
