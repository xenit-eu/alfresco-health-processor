package eu.xenit.alfresco.healthprocessor.indexing.txnaggregation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionAggregationIndexingStrategyStateTest {

    @Test
    void getMapRepresentation() {
        TransactionAggregationIndexingStrategyState state = new TransactionAggregationIndexingStrategyState(1, 2, 3);
        for (int i = 0; i < 4; i ++) state.getTransactionBatchesQueueSize().incrementAndGet();
        Map<String, String> expectedRepresentation = Map.of(
                "current-transaction-id", "1",
                "max-transaction-id", "2",
                "running-transaction-mergers", "3",
                "transaction-batches-queue-size", "4"
        );
        assertEquals(expectedRepresentation, state.getMapRepresentation());
    }
}