package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy.IndexingStrategyKey.SINGLE_TXNS;

@Getter
public class SingleTransactionIndexingConfiguration implements IndexingConfiguration {

    private final static @NonNull String START_TXN_ID_IDENTIFIER = "start-txn-id";
    private final static @NonNull String STOP_TXN_ID_IDENTIFIER = "stop-txn-id";
    private final static @NonNull String BACKGROUND_WORKER_TRANSACTIONS_QUEUE_SIZE_IDENTIFIER = "background-worker-transactions-queue-size";

    private final long startTxnId;
    private final long stopTxnId;
    private final int backgroundWorkerTransactionsQueueSize;
    private final int transactionMinSizeThreshold;
    private final @NonNull Map<String, String> configuration;
    private final @NonNull IndexingStrategy.IndexingStrategyKey indexingStrategy = SINGLE_TXNS;

    public SingleTransactionIndexingConfiguration(long startTxnId, long stopTxnId,
                                                  int backgroundWorkerTransactionsQueueSize,
                                                  int transactionMinSizeThreshold) {
        if (startTxnId > stopTxnId) throw new IllegalArgumentException(String.format("invalid configuration, startTxnId (%d) > stopId (%d)", startTxnId, stopTxnId));
        if (backgroundWorkerTransactionsQueueSize <= 0) throw new IllegalArgumentException("invalid configuration, backgroundWorkerTransactionsQueueSize <= 0");

        this.startTxnId = startTxnId;
        this.stopTxnId = stopTxnId;
        this.backgroundWorkerTransactionsQueueSize = backgroundWorkerTransactionsQueueSize;
        this.transactionMinSizeThreshold = transactionMinSizeThreshold;

        // We don't need to dynamically generate this. We can just create it once.
        this.configuration = Map.of(START_TXN_ID_IDENTIFIER, Long.toString(startTxnId),
                STOP_TXN_ID_IDENTIFIER, Long.toString(stopTxnId),
                BACKGROUND_WORKER_TRANSACTIONS_QUEUE_SIZE_IDENTIFIER, Long.toString(backgroundWorkerTransactionsQueueSize));
    }

    @Nonnull
    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> ret = new HashMap<>();
        ret.put("transaction-min-size-threshold", Long.toString(transactionMinSizeThreshold));
        return ret;
    }

}
