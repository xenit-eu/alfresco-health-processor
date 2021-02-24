package eu.xenit.alfresco.processor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.processor.indexing.IndexingConfiguration.IndexingStrategyKey;
import java.util.Arrays;
import java.util.UUID;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TxnIdBasedIndexingStrategyTest {

    private static final int NUMBER_OF_TEST_REFS = 100;

    private static final NodeRef[] REFS = new NodeRef[NUMBER_OF_TEST_REFS];

    private MockedTrackingComponent trackingComponent;

    @BeforeAll
    static void initializeTestNodeRefs() {
        for (int i = 0; i < NUMBER_OF_TEST_REFS; i++) {
            REFS[i] = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        }
    }

    @BeforeEach
    void setup() {
        trackingComponent = new MockedTrackingComponent();
    }

    @Test
    void getNextNodeIds_defaultConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy();

        assertThat(strategy.getNextNodeIds(6), hasSize(6));
        assertThat(strategy.getNextNodeIds(6), hasSize(4));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(config(2L, 6L, 1000));

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(REFS, 1, 6)));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(config(-1L, 1000L, 2));

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(REFS, 0, 6)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(3));
        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(Arrays.copyOfRange(REFS, 6, 10)));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(5));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize_twoNodesPerTransaction() {
        bulkInitTrackingComponent(10, 2); // = 20 nodes in total
        TxnIdBasedIndexingStrategy strategy = strategy(config(-1L, 1000L, 2));

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
    void reset() {
        trackingComponent.addTransaction(1L, REFS[0], REFS[1], REFS[2]);
        TxnIdBasedIndexingStrategy strategy = strategy();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(Arrays.copyOfRange(REFS, 0, 3)));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));

        trackingComponent.addTransaction(2L, REFS[3], REFS[4]);
        strategy.reset();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(Arrays.copyOfRange(REFS, 0, 5)));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
    }

    private IndexingConfiguration config() {
        return config(-1L, Long.MAX_VALUE, 1000);
    }

    private IndexingConfiguration config(long startTxnId, long stopTxnId, int txnBatchSize) {
        return new IndexingConfiguration(IndexingStrategyKey.TXNID, startTxnId, stopTxnId, txnBatchSize);
    }

    private TxnIdBasedIndexingStrategy strategy() {
        return strategy(config());
    }

    private TxnIdBasedIndexingStrategy strategy(IndexingConfiguration configuration) {
        return new TxnIdBasedIndexingStrategy(configuration, trackingComponent);
    }

    private void bulkInitTrackingComponent(int numberOfTransactions, int numberOfNodesPerTransaction) {
        int nodeRefPointer = 0;
        for (long txnId = 1L; txnId <= numberOfTransactions; txnId++) {
            for (int i = 0; i < numberOfNodesPerTransaction; i++) {
                trackingComponent.addTransaction(txnId, REFS[nodeRefPointer++]);
            }
        }
    }

}
