package eu.xenit.alfresco.processor.indexing;

import eu.xenit.alfresco.processor.PropertyConstants;
import eu.xenit.alfresco.processor.util.PropertyUtil;
import java.util.HashSet;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxnIdBasedIndexingStrategy implements IndexingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TxnIdBasedIndexingStrategy.class);

    private final Queue<NodeRef> nodeIdQueue = new LinkedBlockingQueue<>();
    private long maxTxnIdInclusive;
    private boolean done = false;
    private long nextStartTxnIdToFetch;

    private final Configuration configuration;
    private final TrackingComponent trackingComponent;

    public TxnIdBasedIndexingStrategy(Properties globalProperties, TrackingComponent trackingComponent) {
        this(new Configuration(globalProperties), trackingComponent);
    }

    public TxnIdBasedIndexingStrategy(Configuration configuration, TrackingComponent trackingComponent) {
        this.configuration = configuration;
        this.trackingComponent = trackingComponent;
        reset();
    }

    @Override
    public void reset() {
        nodeIdQueue.clear();
        nextStartTxnIdToFetch = -1L;
        done = false;
        maxTxnIdInclusive = Math.min(trackingComponent.getMaxTxnId(), configuration.getStopTxnId());
    }

    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> ret = new HashSet<>();
        while (!done && nodeIdQueue.size() < amount) {
            fetchMoreNodes();
        }

        for (int i = 0; i < amount; i++) {
            if (nodeIdQueue.peek() != null) {
                ret.add(nodeIdQueue.poll());
            }
        }

        return ret;
    }

    private void fetchMoreNodes() {
        long startTxn = getNextStartTxnIdInclusive();
        long endTxnExclusive = getNextStopTxnIdExclusive();

        logger.debug("Fetching more nodes. startTxn={}, endTxnExclusive={}", startTxn, endTxnExclusive);

        nodeIdQueue.addAll(
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
            return nextStartTxnIdToFetch = Math.max(configuration.getStartTxnId(), 1L);
        }
        return nextStartTxnIdToFetch;
    }

    private long getNextStopTxnIdExclusive() {
        return Math.min(getNextStartTxnIdInclusive() + configuration.getTxnBatchSize(), maxTxnIdInclusive + 1);
    }

    @AllArgsConstructor
    @Getter
    public static class Configuration {

        private final long startTxnId;
        private final long stopTxnId;
        private final long txnBatchSize;

        public Configuration(Properties globalProperties) {
            this(
                    PropertyUtil.getOrDefaultLong(globalProperties, PropertyConstants.PROP_INDEXING_TXNID_START, -1L),
                    PropertyUtil.getOrDefaultLong(globalProperties, PropertyConstants.PROP_INDEXING_TXNID_STOP,
                            Long.MAX_VALUE),
                    PropertyUtil.getOrDefaultLong(globalProperties, PropertyConstants.PROP_INDEXING_TXNID_TXNBATCHSIZE,
                            1000L)
            );
        }
    }

}
