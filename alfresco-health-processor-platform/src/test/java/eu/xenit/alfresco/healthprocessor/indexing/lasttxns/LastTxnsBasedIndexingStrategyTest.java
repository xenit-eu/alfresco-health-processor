package eu.xenit.alfresco.healthprocessor.indexing.lasttxns;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import eu.xenit.alfresco.healthprocessor.indexing.FakeTrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LastTxnsBasedIndexingStrategyTest {

    private FakeTrackingComponent trackingComponent;

    @BeforeEach
    void setup() {
        trackingComponent = new FakeTrackingComponent();
    }

    @Test
    void getNextNodeIds_defaultConfiguration() {
        bulkInitTrackingComponent(10, 1);
        LastTxnsBasedIndexingStrategy strategy = strategy();

        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(strategy.getNextNodeIds(6), hasSize(4));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration() {
        bulkInitTrackingComponent(10, 1);
        LastTxnsBasedIndexingStrategy strategy = strategy(5);

        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 5,10)));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize() {
        bulkInitTrackingComponent(10, 1);
        LastTxnsBasedIndexingStrategy strategy = strategy(5, 2);

        strategy.onStart();

        assertThat(strategy.getNextNodeIds(3), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 7,10)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
        assertThat(strategy.getNextNodeIds(3), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 5,7)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(3));
    }

    @Test
    void start_and_stop() {
        trackingComponent.addTransaction(1L, TestNodeRefs.REFS[0], TestNodeRefs.REFS[1], TestNodeRefs.REFS[2]);
        LastTxnsBasedIndexingStrategy strategy = strategy();
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 0, 3)));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));

        trackingComponent.addTransaction(2L, TestNodeRefs.REFS[3], TestNodeRefs.REFS[4]);
        strategy.onStop();
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 0, 5)));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
    }

    @Test
    void getState() {
        trackingComponent.addTransaction(1L, TestNodeRefs.REFS[0]);
        trackingComponent.addTransaction(2L, TestNodeRefs.REFS[1], TestNodeRefs.REFS[2]);
        LastTxnsBasedIndexingStrategy strategy = strategy(10_000, 1);

        strategy.onStart();

        assertThat(strategy.getState(), hasEntry("nodes-in-queue", "0"));
        assertThat(strategy.getState(), hasEntry("processed-transactions", "0"));
        assertThat(strategy.getState(), hasEntry("next-max-txn-id", "2"));
        assertThat(strategy.getState(), hasEntry("initial-max-txn-id", "2"));

        strategy.getNextNodeIds(1);

        assertThat(strategy.getState(), hasEntry("nodes-in-queue", "1"));
        assertThat(strategy.getState(), hasEntry("processed-transactions", "1"));
        assertThat(strategy.getState(), hasEntry("next-max-txn-id", "1"));
        assertThat(strategy.getState(), hasEntry("initial-max-txn-id", "2"));

    }

    private LastTxnsBasedIndexingStrategy strategy(long lookbackTransactions, long batchSize) {
        return new LastTxnsBasedIndexingStrategy(new LastTxnsIndexingConfiguration(lookbackTransactions, batchSize), trackingComponent);
    }
    private LastTxnsBasedIndexingStrategy strategy(long lookbackTransactions) {
        return strategy(lookbackTransactions, 5000);
    }
    private LastTxnsBasedIndexingStrategy strategy() {
        return strategy(10_000);
    }

    private void bulkInitTrackingComponent(int numberOfTransactions, int numberOfNodesPerTransaction) {
        int nodeRefPointer = 0;
        for (long txnId = 1L; txnId <= numberOfTransactions; txnId++) {
            for (int i = 0; i < numberOfNodesPerTransaction; i++) {
                trackingComponent.addTransaction(txnId, TestNodeRefs.REFS[nodeRefPointer++]);
            }
        }
    }

}
