package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.alfresco.service.cmr.repository.NodeRef;

public class FakeTrackingComponent implements TrackingComponent {

    private final Map<Long, Set<NodeInfo>> transactions = new TreeMap<>();
    private int getNodeForTxnIdsInvocations = 0;
    private int nodeIdCounter = 1;

    public void addTransaction(Long txnId, NodeRef... nodes) {
        this.addTransaction(txnId, Arrays.asList(nodes));
    }

    public void addTransaction(Long txnId, List<NodeRef> nodes) {
        transactions.putIfAbsent(txnId, new HashSet<>());
        transactions.get(txnId).addAll(
                nodes.stream().map(n -> new NodeInfo(txnId, nodeIdCounter++, n)).collect(Collectors.toList())
        );
    }

    @Override
    public long getMaxTxnId() {
        return transactions.keySet().stream().reduce(Math::max).orElseThrow(IllegalStateException::new);
    }

    @Override
    public Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds) {
        getNodeForTxnIdsInvocations++;

        Set<NodeInfo> ret = new HashSet<>();
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
