package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Slf4j
public class SingleTransactionIndexingBackgroundWorker implements Runnable {

    private final @NonNull BlockingDeque<@NonNull Pair<@NonNull Long, @NonNull Set<@NonNull NodeRef>>> buffer;
    private final @NonNull TrackingComponent trackingComponent;
    private final @NonNull SingleTransactionIndexingState state;

    public SingleTransactionIndexingBackgroundWorker(@NonNull TrackingComponent trackingComponent,
                                                     @NonNull SingleTransactionIndexingConfiguration configuration,
                                                     @NonNull SingleTransactionIndexingState state) {
        this.buffer = new LinkedBlockingDeque<>(configuration.getBackgroundWorkerTransactionsQueueSize());
        this.trackingComponent = trackingComponent;
        this.state = state;

        this.state.setCurrentlyProcessedTxnId(-1);
    }

    public @NonNull Pair<@NonNull Long, @NonNull Set<@NonNull NodeRef>> takeNextTransaction() throws InterruptedException {
        Pair<Long, Set<NodeRef>> returnValue = buffer.takeFirst();
        updateBufferState();
        return returnValue;
    }

    @Override
    public void run() {
        try {
            log.debug("The background worker of the SingleTransactionIndexingStrategy has been started.");

            // Thread-safe: the indexer strategy can not increase its current Txn ID if the background worker
            //  hasn't returned anything yet. Just make sure the current & last Txn ID are set properly before executing this code.
            long start = state.getCurrentTxnId();
            long end = state.getLastTxnId();
            for (long i = start; i < end; i++) handleNextTransaction(i);
            signalEnd();

            log.debug("The background worker of the SingleTransactionIndexingStrategy has stopped.");
        } catch (InterruptedException e) {
            log.warn("The background worker of the SingleTransactionIndexingStrategy has been interrupted.", e);
        }
    }

    private void handleNextTransaction(long txnId) throws InterruptedException {
        log.trace("Currently processing transaction with ID ({}).", txnId);
        Set<TrackingComponent.NodeInfo> fetchedNodes = trackingComponent.getNodesForTxnIds(List.of(txnId));
        Set<NodeRef> nodeRefs = fetchedNodes.stream()
                .map(TrackingComponent.NodeInfo::getNodeRef)
                .collect(Collectors.toSet());
        if (!nodeRefs.isEmpty()) updateBuffer(txnId, nodeRefs);
        updateProcessedTxnIdState(txnId);
    }

    private void signalEnd() throws InterruptedException {
        // The empty collection signals to the health processor platform that the indexer is done.
        updateBuffer(Set.of());
        // Leave the last processed transaction ID as is. No particular reason to update it.
    }

    private void updateBuffer(long txnId, @NonNull Set<@NonNull NodeRef> elements) throws InterruptedException {
        buffer.putLast(Pair.of(txnId, elements));
        updateBufferState();
    }

    private void updateBuffer(@NonNull Set<@NonNull NodeRef> elements) throws InterruptedException {
        updateBuffer(-1, elements);
    }

    private void updateBufferState() {
        state.setCurrentlyBackgroundWorkerQueueSize(buffer.size());
    }

    private void updateProcessedTxnIdState(long txnId) {
        state.setCurrentlyProcessedTxnId(txnId);
    }

}
