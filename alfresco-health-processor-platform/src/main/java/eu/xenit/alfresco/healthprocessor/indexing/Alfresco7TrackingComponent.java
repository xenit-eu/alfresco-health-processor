package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.NodeDaoAwareTrackingComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.domain.node.Transaction;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class Alfresco7TrackingComponent implements NodeDaoAwareTrackingComponent {

    private final SearchTrackingComponent trackingComponent;

    private final AbstractNodeDAOImpl nodeDAO;

    private Long lastTxnTime = -1L;

    @Override
    public long getMaxTxnId() {
        return trackingComponent.getMaxTxnId();
    }

    @Override
    public int getTransactionCount() {
        int count = nodeDAO.getTransactionCount();
        log.debug("Found {} transactions", count);
        return count;
    }

    @Override
    public synchronized List<Transaction> getNextTransactions(Integer count) {
        List<Transaction> txns = nodeDAO.selectTxns(lastTxnTime, Long.MAX_VALUE, count,
                null, null, true);
        long newLastTxnTime = txns.stream().mapToLong(Transaction::getCommitTimeMs).max().orElse(Long.MAX_VALUE);
        log.debug("Returning {} transactions from {} to {}", txns, lastTxnTime, newLastTxnTime);
        lastTxnTime = newLastTxnTime;
        return txns;
    }

    @Override
    public Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds) {
        if(txnIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<NodeInfo> ret = new HashSet<>();
        NodeParameters parameters = toNodeParameters(txnIds);
        trackingComponent.getNodes(parameters, node -> {
            // TODO filter out deleted nodes?
            ret.add(new NodeInfo(node.getTransaction().getId(), node.getId(), node.getNodeRef()));
            return true;
        });

        List<String> ids = txnIds.stream()
                .map(l -> Long.toString(l))
                .collect(Collectors.toList());
        log.debug("Returning {} nodes for transactions {}", ret.size(), String.join(",", ids));
        return ret;
    }
}
