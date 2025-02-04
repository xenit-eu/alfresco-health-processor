package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ThresholdIndexingStrategyTransactionIdMerger implements Runnable {

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
            HashSet<NodeRef> bucket = new HashSet<>(configuration.getThreshold());

            List<Transaction> newTransactions;
            while (!(newTransactions = fetcher.getNextTransactions()).isEmpty()) {
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
        bucket.add(node.getNodeRef());

        if (bucket.size() >= configuration.getThreshold()) {
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
        queuedNodes.putLast(Set.of());
    }

}