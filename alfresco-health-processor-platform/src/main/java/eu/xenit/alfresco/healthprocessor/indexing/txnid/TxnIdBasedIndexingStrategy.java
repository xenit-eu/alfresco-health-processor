package eu.xenit.alfresco.healthprocessor.indexing.txnid;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent.NodeInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxnIdBasedIndexingStrategy implements IndexingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TxnIdBasedIndexingStrategy.class);

    private final Queue<NodeInfo> nodeQueue = new PriorityQueue<>();
    private long maxTxnIdInclusive;
    private boolean done = false;
    private long nextStartTxnIdToFetch;

    private final IndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;

    public TxnIdBasedIndexingStrategy(IndexingConfiguration configuration, TrackingComponent trackingComponent) {
        this.configuration = configuration;
        this.trackingComponent = trackingComponent;
    }

    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();

        ret.put("max-txn-id-inclusive", Long.toString(maxTxnIdInclusive));
        ret.put("next-txn-id", Long.toString(nextStartTxnIdToFetch));
        ret.put("nodes-in-queue", Integer.toString(nodeQueue.size()));
        ret.put("done", Boolean.toString(done));

        return ret;
    }

    @Override
    public void onStart() {
        nextStartTxnIdToFetch = -1L;
        done = false;
        maxTxnIdInclusive = Math.min(trackingComponent.getMaxTxnId(), configuration.getStopTxnId());
    }

    @Override
    public void onStop() {
        nodeQueue.clear();
    }

    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> ret = new HashSet<>();
        while (!done && nodeQueue.size() < amount) {
            fetchMoreNodes();
        }

        for (int i = 0; i < amount; i++) {
            if (nodeQueue.peek() != null) {
                ret.add(nodeQueue.poll().getNodeRef());
            }
        }

        return ret;
    }

    private void fetchMoreNodes() {
        long startTxn = getNextStartTxnIdInclusive();
        long endTxnExclusive = getNextStopTxnIdExclusive();

        logger.debug("Fetching more nodes. startTxn={}, endTxnExclusive={}", startTxn, endTxnExclusive);

        nodeQueue.addAll(
                trackingComponent.getNodesForTxnIds(
                        LongStream.range(startTxn, endTxnExclusive).boxed().collect(Collectors.toList())));

        nextStartTxnIdToFetch = endTxnExclusive;
        if (nextStartTxnIdToFetch > maxTxnIdInclusive) {
            logger.debug("nextStartTxnIdToFetch ({}) > maxTxnIdInclusive ({}) -> DONE!",
                    nextStartTxnIdToFetch, maxTxnIdInclusive);
            done = true;
        }
    }

    private long getNextStartTxnIdInclusive() {
        if (nextStartTxnIdToFetch < 1) {
            nextStartTxnIdToFetch = Math.max(configuration.getStartTxnId(), 1L);
        }
        return nextStartTxnIdToFetch;
    }

    private long getNextStopTxnIdExclusive() {
        return Math.min(getNextStartTxnIdInclusive() + configuration.getTxnBatchSize(), maxTxnIdInclusive + 1);
    }

}
