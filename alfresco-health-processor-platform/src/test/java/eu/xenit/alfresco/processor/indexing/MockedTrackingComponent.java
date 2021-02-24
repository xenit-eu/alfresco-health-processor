package eu.xenit.alfresco.processor.indexing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.alfresco.service.cmr.repository.NodeRef;

public class MockedTrackingComponent implements TrackingComponent {

    private final Map<Long, Set<NodeRef>> transactions = new TreeMap<>();
    private int getNodeForTxnIdsInvocations = 0;

    void addTransaction(Long txnId, NodeRef... nodes) {
        this.addTransaction(txnId, Arrays.asList(nodes));
    }

    void addTransaction(Long txnId, List<NodeRef> nodes) {
        transactions.putIfAbsent(txnId, new HashSet<>());
        transactions.get(txnId).addAll(nodes);
    }

    @Override
    public long getMaxTxnId() {
        return transactions.keySet().stream().reduce(Math::max).orElseThrow(IllegalStateException::new);
    }

    @Override
    public Set<NodeRef> getNodesForTxnIds(List<Long> txnIds) {
        getNodeForTxnIdsInvocations++;

        Set<NodeRef> ret = new HashSet<>();
        transactions.forEach((key, value) -> {
            if (txnIds.contains(key)) {
                ret.addAll(value);
            }
        });
        return ret;
    }

    public int numberOfGetNodeForTxnIdsInvocations() {
        return getNodeForTxnIdsInvocations;
    }
}
