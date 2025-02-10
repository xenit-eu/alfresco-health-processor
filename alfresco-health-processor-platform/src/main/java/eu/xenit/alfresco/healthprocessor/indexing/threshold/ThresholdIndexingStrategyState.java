package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThresholdIndexingStrategyState implements MeterBinder {

    private long currentTransactionId = -1;
    private long maxTransactionId = -1;
    private int runningTransactionMergers = 0; // Only adjusted by one thread, so does not need to be atomic.
    private final @NonNull AtomicInteger transactionBatchesQueueSize = new AtomicInteger(0);

    public @NonNull Map<@NonNull String, @NonNull String> getMapRepresentation() {
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

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.current-transaction-id", this, ThresholdIndexingStrategyState::getCurrentTransactionId);
        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.max-transaction-id", this, ThresholdIndexingStrategyState::getMaxTransactionId);
        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.running-transaction-mergers", this, ThresholdIndexingStrategyState::getRunningTransactionMergers);
        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.queued-transaction-batches", transactionBatchesQueueSize, AtomicInteger::get);
    }
}