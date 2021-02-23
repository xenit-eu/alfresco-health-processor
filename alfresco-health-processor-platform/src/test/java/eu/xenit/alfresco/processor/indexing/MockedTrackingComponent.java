package eu.xenit.alfresco.processor.indexing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MockedTrackingComponent implements TrackingComponent {

    private final Map<Long, Set<Long>> transactions = new TreeMap<>();
    private int getNodeForTxnIdsInvocations = 0;

    void addTransactions(Long txnId, Long... nodeIds) {
        this.addTransactions(txnId, Arrays.asList(nodeIds));
    }

    void addTransactions(Long txnId, List<Long> nodeIds) {
        transactions.putIfAbsent(txnId, new TreeSet<>());
        transactions.get(txnId).addAll(nodeIds);
    }

    @Override
    public long getMaxTxnId() {
        return transactions.keySet().stream().reduce(Math::max).orElseThrow(IllegalStateException::new);
    }

    @Override
    public Set<Long> getNodesForTxnIds(List<Long> txnIds) {
        getNodeForTxnIdsInvocations++;

        return txnIds.stream()
                .flatMap(id -> transactions.getOrDefault(id, Collections.emptySet()).stream())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public int numberOfGetNodeForTxnIdsInvocations() {
        return getNodeForTxnIdsInvocations;
    }
}
