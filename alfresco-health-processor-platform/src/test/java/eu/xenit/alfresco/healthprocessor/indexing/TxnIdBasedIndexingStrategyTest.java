package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TxnIdBasedIndexingStrategyTest {

    private FakeTrackingComponent trackingComponent;

    @BeforeEach
    void setup() {
        trackingComponent = new FakeTrackingComponent();
    }

    @Test
    void getNextNodeIds_defaultConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy();
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(strategy.getNextNodeIds(6), hasSize(4));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(IndexingConfigUtil.config(2L, 6L, 1000));
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 1, 6)));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_startIdLargerThenStopId() {
        bulkInitTrackingComponent(10, 1);

        assertThrows(IllegalArgumentException.class, () -> strategy(IndexingConfigUtil.config(6L, 2L, 1000)));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(IndexingConfigUtil.config(-1L, 1000L, 2));
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 0, 6)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(3));
        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 6, 10)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(5));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize_twoNodesPerTransaction() {
        bulkInitTrackingComponent(10, 2); // = 20 nodes in total
        TxnIdBasedIndexingStrategy strategy = strategy(IndexingConfigUtil.config(-1L, 1000L, 2));
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(3));
        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(5));
        assertThat(strategy.getNextNodeIds(6), hasSize(2));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(5));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
    }

    @Test
    void start_and_stop() {
        trackingComponent.addTransaction(1L, TestNodeRefs.REFS[0], TestNodeRefs.REFS[1], TestNodeRefs.REFS[2]);
        TxnIdBasedIndexingStrategy strategy = strategy();
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

    private TxnIdBasedIndexingStrategy strategy() {
        return strategy(IndexingConfigUtil.defaultConfig());
    }

    private TxnIdBasedIndexingStrategy strategy(IndexingConfiguration configuration) {
        return new TxnIdBasedIndexingStrategy(configuration, trackingComponent);
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
