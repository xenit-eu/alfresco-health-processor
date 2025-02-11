package eu.xenit.alfresco.healthprocessor.indexing.txnaggregation;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;

@Data
public class TransactionAggregationIndexingStrategyConfiguration implements IndexingConfiguration {

    private final int transactionsBackgroundWorkers;
    private final int transactionsBatchSize;
    private final int threshold;
    private final int minTransactionId;
    private final int maxTransactionId;
    private final @NonNull Map<@NonNull String, @NonNull String> configuration;

    public TransactionAggregationIndexingStrategyConfiguration(int transactionsBackgroundWorkers, int transactionsBatchSize,
                                                               int threshold, int minTransactionId, int maxTransactionId) {
        this.transactionsBackgroundWorkers = transactionsBackgroundWorkers;
        this.transactionsBatchSize = transactionsBatchSize;
        this.threshold = threshold;
        this.minTransactionId = minTransactionId;
        this.maxTransactionId = maxTransactionId;

        this.configuration = Map.of(
                "transactions-background-workers", String.valueOf(transactionsBackgroundWorkers),
                "transactions-batch-size", String.valueOf(transactionsBatchSize),
                "threshold", String.valueOf(threshold),
                "min-transaction-id", String.valueOf(minTransactionId),
                "max-transaction-id", String.valueOf(maxTransactionId)
        );
    }

    @Override
    public @NonNull IndexingStrategy.IndexingStrategyKey getIndexingStrategy() {
        return IndexingStrategy.IndexingStrategyKey.TXN_AGGREGATION;
    }

}