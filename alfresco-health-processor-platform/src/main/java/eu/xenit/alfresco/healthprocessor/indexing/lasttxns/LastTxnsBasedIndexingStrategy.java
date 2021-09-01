package eu.xenit.alfresco.healthprocessor.indexing.lasttxns;

import eu.xenit.alfresco.healthprocessor.indexing.NullIndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleIndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent.NodeInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;

@Slf4j
@RequiredArgsConstructor
public class LastTxnsBasedIndexingStrategy implements IndexingStrategy {

    private final LastTxnsIndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;
    private final Queue<NodeInfo> nodeQueue = new PriorityQueue<>(
            Collections.reverseOrder(Comparator.comparingLong((NodeInfo::getTxnId))));
    private long initialMaxTxId;
    private long nextMaxTxId;
    private long processedTransactions;

    private IndexingProgress indexingProgress = NullIndexingProgress.getInstance();

    @Override
    public void onStart() {
        nodeQueue.clear();
        initialMaxTxId = nextMaxTxId = trackingComponent.getMaxTxnId();
        processedTransactions = 0;
        indexingProgress = new SimpleIndexingProgress(0, Math.min(initialMaxTxId, configuration.getLookbackTransactions()), () -> processedTransactions);
    }

    @Nonnull
    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> ret = new HashSet<>();

        while (
                nextMaxTxId > 0
                        && processedTransactions < configuration.getLookbackTransactions()
                        && nodeQueue.size() < amount
        ) {
            fetchMoreNodes();
        }

        for (int i = 0; i < amount; i++) {
            NodeInfo nodeInfo = nodeQueue.poll();
            if (nodeInfo == null) {
                break;
            }
            ret.add(nodeInfo.getNodeRef());
        }
        return ret;
    }

    private void fetchMoreNodes() {
        long endTxIdExclusive = nextMaxTxId + 1;
        long transactionsToLookBack = Math.min(
                configuration.getBatchSize(),
                configuration.getLookbackTransactions() - processedTransactions
        );
        assert transactionsToLookBack > 0;
        long startTxId = Math.max(0, endTxIdExclusive - transactionsToLookBack);
        assert startTxId < endTxIdExclusive;
        log.debug("Fetching more nodes. startTxId={}, endTxIdExclusive={}", startTxId, endTxIdExclusive);
        Set<NodeInfo> nodeInfo = trackingComponent.getNodesForTxnIds(
                LongStream.range(startTxId, endTxIdExclusive).boxed().collect(Collectors.toList())
        );

        long uniqueTransactions = nodeInfo.stream().map(NodeInfo::getTxnId).distinct().count();
        log.debug("Processed {} unique transactions", uniqueTransactions);
        processedTransactions += uniqueTransactions;
        nextMaxTxId = startTxId - 1;

        nodeQueue.addAll(nodeInfo);
    }

    @Override
    public void onStop() {
        log.info("Processed nodes from transaction {} until transaction {}. #{} transactions with nodes",
                nextMaxTxId + 1, initialMaxTxId, processedTransactions);
        indexingProgress = NullIndexingProgress.getInstance();
    }

    @Nonnull
    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();

        ret.put("nodes-in-queue", Integer.toString(nodeQueue.size()));
        ret.put("processed-transactions", Long.toString(processedTransactions));
        ret.put("next-max-txn-id", Long.toString(nextMaxTxId));
        ret.put("initial-max-txn-id", Long.toString(initialMaxTxId));

        return ret;
    }

    @Nonnull
    @Override
    public IndexingProgress getIndexingProgress() {
        return indexingProgress;
    }
}
