package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.TrackerInfo;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class NodeTxService {
    private static final Logger logger = LoggerFactory.getLogger(NodeTxService.class);

    protected final SOLRTrackingComponent tracker;

    public List<NodeRef> getNodeReferences(Transaction tx) {
        return getNodeReferences(Collections.singletonList(tx));
    }

    public List<NodeRef> getNodeReferences(List<Transaction> txns) {
        try {
            List<Long> txnIds = txns.stream().map(Transaction::getId).collect(Collectors.toList());
            String txnIdsString = txnIds.stream().map(Object::toString)
                                    .collect(Collectors.joining(","));
            logger.trace("Fetching txs: {}", txnIdsString);
            NodeParameters params = new NodeParameters();
            params.setTransactionIds(txnIds);
            final List<NodeRef> nodeRefs = new ArrayList<>();
            SOLRTrackingComponent.NodeQueryCallback callback = node -> {
                nodeRefs.add(node.getNodeRef());
                return true;
            };
            tracker.getNodes(params, callback);
            String nodeRefsString = nodeRefs.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            logger.trace("Received nodes: {}", nodeRefsString);
            return nodeRefs;
        } catch (Exception ex) {
            logger.error("Impossible to read tracker info: " + ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    public List<Transaction> getNodeTransactions(TrackerInfo trackerInfo, int txnLimit, int timeIncrementSeconds) {
        long maxCommitTimeEpoch = trackerInfo.getCommitTimeMs() + (timeIncrementSeconds * 1000L);
        logger.trace("Fetching a maximum of {} txs from {} to {}, commit time from {} to {}",
                txnLimit, trackerInfo.getTransactionId(), Long.MAX_VALUE,
                toString(trackerInfo.getCommitTimeMs()), toString(maxCommitTimeEpoch));
        return tracker.getTransactions(
        trackerInfo.getTransactionId(), trackerInfo.getCommitTimeMs(),
        Long.MAX_VALUE, maxCommitTimeEpoch,
        txnLimit
        );
    }

    private String toString(long epoch) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
                .toString();
    }
}
