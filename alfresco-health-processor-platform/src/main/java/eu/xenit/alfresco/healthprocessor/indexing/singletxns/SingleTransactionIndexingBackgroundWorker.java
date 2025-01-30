package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Slf4j
public class SingleTransactionIndexingBackgroundWorker implements Runnable {

    private final @NonNull BlockingDeque<@NonNull Set<@NonNull NodeRef>> buffer;
    private final @NonNull TrackingComponent trackingComponent;
    private final @NonNull SingleTransactionIndexingConfiguration configuration;
    private final @NonNull SingleTransactionIndexingState state;

    public SingleTransactionIndexingBackgroundWorker(@NonNull TrackingComponent trackingComponent,
                                                     @NonNull SingleTransactionIndexingConfiguration configuration,
                                                     @NonNull SingleTransactionIndexingState state) {
        this.buffer = new LinkedBlockingDeque<>(configuration.getBackgroundWorkerTransactionsQueueSize());
        this.trackingComponent = trackingComponent;
        this.configuration = configuration;
        this.state = state;

        this.state.setCurrentlyProcessedTxnId(-1);
    }

    public @NonNull Set<@NonNull NodeRef> takeNextTransaction() throws InterruptedException {
        Set<@NonNull NodeRef> returnValue = buffer.takeFirst();

        synchronized (state) {
            state.setCurrentlyBackgroundWorkerQueueSize(buffer.size());
        }

        return returnValue;
    }

    @Override
    public void run() {
        log.debug("The background worker of the SingleTransactionIndexingStrategy has been started.");

        // Thread-safe: the indexer strategy can not increase its current Txn ID if the background worker
        //  hasn't returned anything yet.
        long start = state.getCurrentTxnId();
        long end = state.getLastTxnId();
        for (long i = start; i < end; i ++) handleNextTransaction(i);
        signalEnd();

        log.debug("The background worker of the SingleTransactionIndexingStrategy has stopped.");
    }

    private void handleNextTransaction(long txnId) {
        log.trace("Currently processing transaction with ID ({}).", txnId);
        Set<TrackingComponent.NodeInfo> fetchedNodes = trackingComponent.getNodesForTxnIds(List.of(txnId));
        Set<NodeRef> nodeRefs = fetchedNodes.stream()
                .map(TrackingComponent.NodeInfo::getNodeRef)
                .collect(Collectors.toSet());
        if (!nodeRefs.isEmpty()) updateBuffer(nodeRefs);
        signalProcessedTxnId(txnId);
    }

    @Synchronized("state")
    private void signalProcessedTxnId(long txnId) {
        state.setCurrentlyProcessedTxnId(txnId);
    }

    private void signalEnd() {
        // The empty collection signals to the health processor platform that the indexer is done.
        updateBuffer(Set.of());
        // Leave the last processed transaction ID as is. No particular reason to update it.
    }

    @SneakyThrows(InterruptedException.class) // Never happens in the single transaction indexing strategy.
    private void updateBuffer(@NonNull Set<@NonNull NodeRef> elements) {
        buffer.putLast(elements);

        synchronized (state) {
            state.setCurrentlyBackgroundWorkerQueueSize(buffer.size());
        }
    }

}
