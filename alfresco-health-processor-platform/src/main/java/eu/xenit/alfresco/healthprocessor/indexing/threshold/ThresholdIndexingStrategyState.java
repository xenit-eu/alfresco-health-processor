package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ThresholdIndexingStrategyState {

    private long currentTransactionId = -1;
    private long maxTransactionId = -1;
    private int runningTransactionMergers = 0; // Only adjusted by one thread, so does not need to be atomic.
    private final @NonNull AtomicInteger transactionBatchesQueueSize = new AtomicInteger(0);

    public @NonNull Map<String, String> getMapRepresentation() {
        return Map.of(
                "current-transaction-id", String.valueOf(currentTransactionId),
                "max-transaction-id", String.valueOf(maxTransactionId),
                "running-transaction-mergers", String.valueOf(runningTransactionMergers),
                "transaction-batches-queue-size", String.valueOf(transactionBatchesQueueSize.get())
        );
    }

    public void decrementRunningTransactionMergers() {
        runningTransactionMergers--;
    }

}