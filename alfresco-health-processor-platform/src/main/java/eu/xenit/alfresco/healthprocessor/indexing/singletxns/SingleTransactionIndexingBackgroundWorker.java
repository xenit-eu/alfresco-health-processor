package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import com.google.common.base.Strings;
import eu.xenit.alfresco.healthprocessor.NodeDaoAwareTrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.Transaction;
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
    private final @NonNull NodeDaoAwareTrackingComponent trackingComponent;
    private final @NonNull SingleTransactionIndexingState state;
    private final int aggregateThreshhold;

    public SingleTransactionIndexingBackgroundWorker(@NonNull NodeDaoAwareTrackingComponent trackingComponent,
                                                     @NonNull SingleTransactionIndexingConfiguration configuration,
                                                     @NonNull SingleTransactionIndexingState state) {
        this.buffer = new LinkedBlockingDeque<>(configuration.getBackgroundWorkerTransactionsQueueSize());
        this.trackingComponent = trackingComponent;
        this.state = state;
        String threshold = configuration.getConfiguration().get("transaction-min-size-threshold");
        this.aggregateThreshhold = Strings.isNullOrEmpty(threshold) ? 0 : Integer.parseInt(threshold);

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

//            // Thread-safe: the indexer strategy can not increase its current Txn ID if the background worker
//            //  hasn't returned anything yet. Just make sure the current & last Txn ID are set properly before executing this code.
            int maxTxnCount = trackingComponent.getTransactionCount();
            for(int txnCount = 1; txnCount <= maxTxnCount; txnCount++) {
                log.debug("Getting transaction {}", txnCount);
                List<Transaction> transactions = trackingComponent.getNextTransactions(1);
                if (transactions == null || transactions.isEmpty()) return; // early exit, nothing more to process

                Transaction currentlyProcessedTransaction = transactions.get(0);
                long txnId = currentlyProcessedTransaction.getId();
                log.trace("Currently processing transaction with ID ({} @ {}).",
                        txnId, currentlyProcessedTransaction.getCommitTimeMs());
                handleNextTransaction(txnId);
            }

            log.debug("The background worker of the SingleTransactionIndexingStrategy has stopped.");
        } catch (InterruptedException e) {
            log.warn("The background worker of the SingleTransactionIndexingStrategy has been interrupted.", e);
        } finally {
            try {
                signalEnd();
            } catch (InterruptedException e) {
                log.error("The background worker of the SingleTransactionIndexingStrategy has been interrupted while signaling the end.", e);
            }
        }
    }

    private void handleNextTransaction(long txnId) throws InterruptedException {
        Set<TrackingComponent.NodeInfo> fetchedNodes = trackingComponent.getNodesForTxnIds(List.of(txnId));
        state.setCurrentTxnId(txnId);
        if(fetchedNodes.size() <= aggregateThreshhold) {
            return;
        }
        Set<NodeRef> nodeRefs = fetchedNodes.stream()
                .map(TrackingComponent.NodeInfo::getNodeRef)
                .collect(Collectors.toSet());
        updateBuffer(txnId, nodeRefs);
        updateProcessedTxnIdState(txnId);
    }

    private void signalEnd() throws InterruptedException {
        // The Txn ID of -1 signals to the health processor platform that the indexer is done.
        updateBuffer(-1, Set.of());
        // Leave the last processed transaction ID as is. No particular reason to update it.
    }

    private void updateBuffer(long txnId, @NonNull Set<@NonNull NodeRef> elements) throws InterruptedException {
        buffer.putLast(Pair.of(txnId, elements));
        updateBufferState();
    }

    private void updateBufferState() {
        state.setCurrentlyBackgroundWorkerQueueSize(buffer.size());
    }

    private void updateProcessedTxnIdState(long txnId) {
        state.setCurrentlyProcessedTxnId(txnId);
    }

}
