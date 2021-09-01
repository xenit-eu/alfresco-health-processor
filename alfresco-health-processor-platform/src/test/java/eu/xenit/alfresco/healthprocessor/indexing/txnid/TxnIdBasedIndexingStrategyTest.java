package eu.xenit.alfresco.healthprocessor.indexing.txnid;

import static eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy.ATTR_KEY_LAST_PROCESSED_TXN_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.xenit.alfresco.healthprocessor.indexing.FakeTrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfigUtil;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import eu.xenit.alfresco.healthprocessor.util.InMemoryAttributeStore;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TxnIdBasedIndexingStrategyTest {

    private FakeTrackingComponent trackingComponent;
    private AttributeStore attributeStore;

    @BeforeEach
    void setup() {
        trackingComponent = new FakeTrackingComponent();
        attributeStore = new InMemoryAttributeStore();
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

        assertThrows(IllegalArgumentException.class,
                () -> new TxnIdIndexingConfiguration(6L, 2L, 1000));
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
    void getNextNodeIds_persistentState_pickupFromPreviousCycle() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy();
        attributeStore.setAttribute(5L, ATTR_KEY_LAST_PROCESSED_TXN_ID);
        strategy.onStart();

        assertThat(strategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 4, 8)));
        assertThat(strategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 8, 10)));
        assertThat(strategy.getNextNodeIds(4), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_persistentState_ensureStateIsSaved() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy();
        strategy.onStart();

        assertLastProcessedAttributeValueEquals(null);
        assertThat(strategy.getNextNodeIds(3), hasSize(3));
        assertLastProcessedAttributeValueEquals(1L);
        assertThat(strategy.getNextNodeIds(3), hasSize(3));
        assertLastProcessedAttributeValueEquals(4L);
        assertThat(strategy.getNextNodeIds(3), hasSize(3));
        assertLastProcessedAttributeValueEquals(7L);
        assertThat(strategy.getNextNodeIds(3), hasSize(1));
        assertLastProcessedAttributeValueEquals(10L);
        assertThat(strategy.getNextNodeIds(3), is(empty()));

        strategy.onStop();
        assertLastProcessedAttributeValueEquals(null);
    }

    @Test
    void getNextNodeIds_persistentState_fullScenario() {
        bulkInitTrackingComponent(10, 1);

        TxnIdBasedIndexingStrategy originalStrategy = strategy();
        originalStrategy.onStart();
        assertThat(originalStrategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 0, 4)));
        assertThat(originalStrategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 4, 8)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));

        // *** CRASH ***
        // The original TxnIdBasedIndexingStrategy does not know if the second batch is successfully processed.
        // After a restart, another TxnIdBasedIndexingStrategy picks up the state and continues, starting with the
        // last processed batch.

        TxnIdBasedIndexingStrategy secondStrategy = new TxnIdBasedIndexingStrategy(IndexingConfigUtil.defaultConfig(),
                trackingComponent, attributeStore);
        secondStrategy.onStart();

        assertThat(secondStrategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 4, 8)));
        assertThat(secondStrategy.getNextNodeIds(4), containsInAnyOrder(Arrays.copyOfRange(TestNodeRefs.REFS, 8, 10)));
        assertThat(secondStrategy.getNextNodeIds(4), is(empty()));

        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
    }

    private void assertLastProcessedAttributeValueEquals(Long expected) {
        Long actual = attributeStore.getAttribute(ATTR_KEY_LAST_PROCESSED_TXN_ID);
        assertThat(actual, is(equalTo(expected)));
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
    void getProgress_limitedByConfiguration() {
        bulkInitTrackingComponent(10, 3); // 30 nodes in total

        TxnIdBasedIndexingStrategy strategy = strategy(IndexingConfigUtil.config(-1, Long.MAX_VALUE, 2)); // 6 nodes per fetch
        assertThat(strategy.getCycleProgress().isUnknown(), is(true));

        strategy.onStart();
        assertThat(strategy.getCycleProgress().getProgress(), is(0.0f));

        strategy.getNextNodeIds(2); // Fetched 2/10 transactions (batch size)
        assertThat((double)strategy.getCycleProgress().getProgress(), is(closeTo(0.2, 0.0001)));

        strategy.getNextNodeIds(1); // Processed 3 in total (no extra tx fetched)
        assertThat((double)strategy.getCycleProgress().getProgress(), is(closeTo(0.2, 0.0001)));

        strategy.getNextNodeIds(6); // Processed 9 in total (2 extra tx fetched)
        assertThat((double)strategy.getCycleProgress().getProgress(), is(closeTo(0.4, 0.0001)));

        strategy.getNextNodeIds(10); // Processed 19 in total (4 extra tx fetched)
        assertThat((double)strategy.getCycleProgress().getProgress(), is(closeTo(0.8, 0.0001)));

        strategy.getNextNodeIds(20); // Processed 29 in total (all Tx fetched)
        assertThat((double)strategy.getCycleProgress().getProgress(), is(closeTo(1.0, 0.0001)));

        strategy.getNextNodeIds(100); // Processed 30 in total
        assertThat((double)strategy.getCycleProgress().getProgress(), is(1.0));
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

    private TxnIdBasedIndexingStrategy strategy(TxnIdIndexingConfiguration configuration) {
        return new TxnIdBasedIndexingStrategy(configuration, trackingComponent, attributeStore);
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
