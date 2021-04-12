package eu.xenit.alfresco.healthprocessor.indexing.txnid;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent.NodeInfo;
import eu.xenit.alfresco.healthprocessor.util.AttributeHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class TxnIdBasedIndexingStrategy implements IndexingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TxnIdBasedIndexingStrategy.class);

    static final String ATTR_KEY_LAST_PROCESSED_TXN_ID = "last-processed-txn-id";

    private final Queue<NodeInfo> nodeQueue = new PriorityQueue<>();
    private long maxTxnIdInclusive;
    private boolean done = false;
    private long nextStartTxnIdToFetch;

    private final IndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;
    private final AttributeHelper attributeHelper;

    @Override
    public Map<String, String> getState() {
        Map<String, String> ret = new HashMap<>();

        ret.put("max-txn-id-inclusive", Long.toString(maxTxnIdInclusive));
        ret.put("next-txn-id", Long.toString(nextStartTxnIdToFetch));
        ret.put("nodes-in-queue", Integer.toString(nodeQueue.size()));
        ret.put("fetching-nodes-done", Boolean.toString(done));

        return ret;
    }

    @Override
    public void onStart() {
        done = false;
        nodeQueue.clear();
        initializeStartTxnId();
        initializeMaxTxnId();
    }

    @Override
    public void onStop() {
        attributeHelper.removeAttributes(ATTR_KEY_LAST_PROCESSED_TXN_ID);
    }

    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> ret = new HashSet<>();
        while (!done && nodeQueue.size() < amount) {
            fetchMoreNodes();
        }

        for (int i = 0; i < amount; i++) {
            NodeInfo nodeInfo = nodeQueue.poll();
            if (nodeInfo == null) {
                break;
            }
            if (i == 0) {
                attributeHelper.setAttribute(nodeInfo.getTxnId(), ATTR_KEY_LAST_PROCESSED_TXN_ID);
            }
            ret.add(nodeInfo.getNodeRef());
        }

        return ret;
    }

    private void initializeStartTxnId() {
        Long lastProcessedTxnId = attributeHelper.getAttributeOrDefault(ATTR_KEY_LAST_PROCESSED_TXN_ID, 1L);
        nextStartTxnIdToFetch = Math.max(configuration.getStartTxnId(), lastProcessedTxnId);
    }

    private void initializeMaxTxnId() {
        maxTxnIdInclusive = Math.min(trackingComponent.getMaxTxnId(), configuration.getStopTxnId());
    }

    private void fetchMoreNodes() {
        long startTxn = nextStartTxnIdToFetch;
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

    private long getNextStopTxnIdExclusive() {
        return Math.min(nextStartTxnIdToFetch + configuration.getTxnBatchSize(), maxTxnIdInclusive + 1);
    }

}
