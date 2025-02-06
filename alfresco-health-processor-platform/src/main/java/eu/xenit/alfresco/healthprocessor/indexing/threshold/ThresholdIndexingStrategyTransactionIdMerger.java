package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ThresholdIndexingStrategyTransactionIdMerger implements Runnable {

    private static final @NonNull Set<@NonNull StoreRef> WORKSPACE_AND_ARCHIVE_STORE_REFS = Set.of(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);

    private final @NonNull ThresholdIndexingStrategyTransactionIdFetcher fetcher;
    private final @NonNull BlockingDeque<Set<NodeRef>> queuedNodes;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;
    private final @NonNull NodeParameters nodeParameters = new NodeParameters();
    private final @NonNull SearchTrackingComponent searchTrackingComponent;

    @Override
    public void run() {
        try {
            log.debug("Starting ({}).", Thread.currentThread().getName());
            HashSet<NodeRef> bucket = new HashSet<>(configuration.getThreshold());

            List<Transaction> newTransactions;
            while (!(newTransactions = fetcher.getNextTransactions()).isEmpty()) {
                // Fetch the nodes of the new transactions.
                // Even though we fetch multiple transactions at once for IO-efficiency, we still need to keep track
                // of which nodes belong to which transaction.
                // Transactions that already contain #threshold nodes shouldn't be part of the merging process, since
                // there is no point (they are already large enough).
                log.trace("Fetched a new batch of ({}) transaction(s) from the transaction fetcher to process.", newTransactions.size());
                nodeParameters.setTransactionIds(newTransactions.stream().map(Transaction::getId).collect(Collectors.toList()));
                HashMap<Long, HashSet<Node>> fetchedTransactionNodes = new HashMap<>(nodeParameters.getTransactionIds().size());
                searchTrackingComponent.getNodes(nodeParameters, node ->
                        fetchedTransactionNodes.computeIfAbsent(node.getTransaction().getId(), ignored -> new HashSet<>()).add(node));

                // Start filtering & merging the nodes into the bucket.
                handleTransactionNodes(bucket, fetchedTransactionNodes);
            }
        } catch (Exception e) {
            log.warn("({}) received an interrupt signal, which was unexpected. Trying to signal the end of the merger thread.", Thread.currentThread().getName(), e);
        } finally {
            try {
                signalEnd();
            } catch (InterruptedException e) {
                log.error("({}) received an interrupt signal while trying to signal the end of the merger thread. " +
                        "The merger thread can not recover from this.", Thread.currentThread().getName(), e);
            }
        }
    }

    private void handleTransactionNodes(@NonNull HashSet<@NonNull NodeRef> bucket,
                                        @NonNull HashMap<@NonNull Long, @NonNull HashSet<@NonNull Node>> transactions) throws InterruptedException {
        for (var entry : transactions.entrySet()) {
            if (entry.getValue().size() >= configuration.getThreshold()) {
                log.debug("Transaction ({}) contains more than the threshold amount of nodes ({}). Skipping the transaction.", entry.getKey(), entry.getValue().size());
                continue;
            }

            for (Node node : entry.getValue()) handleTransactionNode(bucket, node);
        }
    }

    private void handleTransactionNode(@NonNull HashSet<@NonNull NodeRef> bucket, @NonNull Node node) throws InterruptedException {
        if (!WORKSPACE_AND_ARCHIVE_STORE_REFS.contains(node.getStore().getStoreRef())) return;

        bucket.add(node.getNodeRef());
        if (bucket.size() >= configuration.getThreshold()) {
            log.debug("Bucket full. Queuing bucket of size ({}).", bucket.size());
            HashSet<NodeRef> copy = new HashSet<>(bucket);
            queuedNodes.putLast(copy);
            bucket.clear();
        }
    }

    private void signalEnd() throws InterruptedException {
        log.debug("({}) received an end signal from the transaction fetcher. Signaling the end to the main indexing strategy.", Thread.currentThread().getName());
        queuedNodes.putLast(Set.of());
    }

}