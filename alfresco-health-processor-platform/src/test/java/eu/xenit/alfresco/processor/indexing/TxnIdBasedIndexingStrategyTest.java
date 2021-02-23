package eu.xenit.alfresco.processor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.processor.indexing.TxnIdBasedIndexingStrategy.Configuration;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TxnIdBasedIndexingStrategyTest {

    private MockedTrackingComponent trackingComponent;

    @BeforeEach
    void setup() {
        trackingComponent = new MockedTrackingComponent();
    }

    @Test
    void getNextNodeIds_defaultConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy();

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(1000L, 2000L, 3000L, 4000L, 5000L, 6000L));
        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(7000L, 8000L, 9000L, 10000L));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(new Configuration(2L, 6L, 1000L));

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(2000L, 3000L, 4000L, 5000L, 6000L));
        assertThat(strategy.getNextNodeIds(6), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize() {
        bulkInitTrackingComponent(10, 1);
        TxnIdBasedIndexingStrategy strategy = strategy(new Configuration(-1L, 1000L, 2L));

        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(1000L, 2000L, 3000L, 4000L, 5000L, 6000L));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(3));
        assertThat(strategy.getNextNodeIds(6), containsInAnyOrder(7000L, 8000L, 9000L, 10000L));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(5));
    }

    @Test
    void getNextNodeIds_limitedByConfiguration_txnBatchSize_twoNodesPerTransaction() {
        bulkInitTrackingComponent(10, 2); // = 20 nodes in total
        TxnIdBasedIndexingStrategy strategy = strategy(new Configuration(-1L, 1000L, 2L));

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
        trackingComponent.addTransactions(1L, 10L, 11L, 12L);
        TxnIdBasedIndexingStrategy strategy = strategy();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(10L, 11L, 12L));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(1));

        trackingComponent.addTransactions(2L, 20L, 21L);
        strategy.reset();

        assertThat(strategy.getNextNodeIds(100), containsInAnyOrder(10L, 11L, 12L, 20L, 21L));
        assertThat(strategy.getNextNodeIds(100), is(empty()));
        assertThat(trackingComponent.numberOfGetNodeForTxnIdsInvocations(), is(2));
    }

    private TxnIdBasedIndexingStrategy strategy() {
        return strategy(new Configuration(new Properties()));
    }

    private TxnIdBasedIndexingStrategy strategy(Configuration configuration) {
        return new TxnIdBasedIndexingStrategy(configuration, trackingComponent);
    }

    private void bulkInitTrackingComponent(int numberOfTransactions, int numberOfNodesPerTransaction) {
        for (long l = 1L; l <= numberOfTransactions; l++) {
            long nodeStart = l * 1000;
            trackingComponent.addTransactions(l,
                    LongStream.range(nodeStart, nodeStart + numberOfNodesPerTransaction)
                            .boxed()
                            .collect(Collectors.toList()));
        }
    }

}
