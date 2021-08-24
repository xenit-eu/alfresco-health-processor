package eu.xenit.alfresco.healthprocessor.indexing.lasttxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
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

    private long startTxId;
    private long currentTxId;
    private long processedTransactions;

    private final Queue<NodeInfo> nodeQueue = new PriorityQueue<>(
            Collections.reverseOrder(Comparator.comparingLong((NodeInfo::getTxnId))));

    @Override
    public void onStart() {
        nodeQueue.clear();
        startTxId = currentTxId = trackingComponent.getMaxTxnId();
        processedTransactions = 0;
    }

    @Nonnull
    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> ret = new HashSet<>();

        while(currentTxId > 0 && processedTransactions < configuration.getLookbackTransactions() && nodeQueue.size() < amount) {
            fetchMoreData();
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

    private void fetchMoreData() {
        long startTxId = Math.max(0, currentTxId - Math.min(configuration.getBatchSize(), configuration.getLookbackTransactions() - processedTransactions));
        log.debug("Fetching more nodes. startTxId={}, endTxIdExclusive={}", startTxId, currentTxId);
        Set<NodeInfo> nodeInfo = trackingComponent.getNodesForTxnIds(
                LongStream.range(startTxId, currentTxId).boxed().collect(Collectors.toList())
        );

        long uniqueTransactions = nodeInfo.stream().map(NodeInfo::getTxnId).distinct().count();
        log.debug("Processed {} unique transactions", uniqueTransactions);
        processedTransactions += uniqueTransactions;
        currentTxId = startTxId;

        nodeQueue.addAll(nodeInfo);
    }

    @Override
    public void onStop() {
        log.info("Processed nodes from transaction {} until transaction {}. #{} transactions with nodes", currentTxId, startTxId, processedTransactions);
    }

    @Nonnull
    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();

        ret.put("nodes-in-queue", Integer.toString(nodeQueue.size()));
        ret.put("processed-transactions", Long.toString(processedTransactions));
        ret.put("current-txn-id", Long.toString(currentTxId));
        ret.put("start-txn-id", Long.toString(startTxId));

        return ret;
    }
}
