package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ThresholdIndexingStrategyTransactionIdMerger implements Runnable {

    private static final @NonNull Set<StoreRef> WORKSPACE_AND_ARCHIVE_STORE_REFS = Set.of(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);

    private final @NonNull ThresholdIndexingStrategyTransactionIdFetcher fetcher;
    private final @NonNull BlockingDeque<Set<NodeRef>> queuedNodes;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;
    private final @NonNull NodeParameters nodeParameters = new NodeParameters();
    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull ThresholdIndexingStrategyState state;

    @Override
    @SneakyThrows(InterruptedException.class)
    public void run() {
        try {
            log.debug("Starting ({}).", Thread.currentThread().getName());
            HashSet<NodeRef> bucket = new HashSet<>(configuration.getThreshold());

            List<Transaction> newTransactions;
            while (!(newTransactions = fetcher.getNextTransactions()).isEmpty()) {
                log.trace("Fetched a new batch of ({}) transaction(s) from the transaction fetcher to process.", newTransactions.size());
                nodeParameters.setTransactionIds(newTransactions.stream().map(Transaction::getId).collect(Collectors.toList()));
                searchTrackingComponent.getNodes(nodeParameters, node -> handleNode(bucket, node));
                updateState(newTransactions);
            }
        } finally {
            signalEnd();
        }
    }

    @SneakyThrows(InterruptedException.class)
    private boolean handleNode(@NonNull HashSet<NodeRef> bucket, @NonNull Node node) {
        if (!WORKSPACE_AND_ARCHIVE_STORE_REFS.contains(node.getStore().getStoreRef())) return true;

        bucket.add(node.getNodeRef());
        if (bucket.size() >= configuration.getThreshold()) {
            log.debug("Bucket full. Queuing bucket of size ({}).", bucket.size());
            HashSet<NodeRef> copy = new HashSet<>(bucket);
            queuedNodes.putLast(copy);
            bucket.clear();
        }

        return true;
    }

    @Synchronized("state")
    private void updateState(@NonNull List<@NonNull Transaction> newTransactions) {
        // Not really necessary. Just to give the admin some insight in the progress.
        long maxTransactionId = newTransactions.get(newTransactions.size() - 1).getId();
        state.setCurrentTransactionId(Math.max(state.getCurrentTransactionId(), maxTransactionId));
    }

    private void signalEnd() throws InterruptedException {
        log.debug("({}) received an end signal from the transaction fetcher. Signaling the end to the main indexing strategy.", Thread.currentThread().getName());
        queuedNodes.putLast(Set.of());
    }

}