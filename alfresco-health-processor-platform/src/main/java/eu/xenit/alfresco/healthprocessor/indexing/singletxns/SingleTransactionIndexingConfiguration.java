package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Map;

@Getter
@AllArgsConstructor
public class SingleTransactionIndexingConfiguration implements IndexingConfiguration {

    long startTxnId;
    long stopTxnId;

    public void setStartTxnId(long startTxnId) {
        guaranteeStartTxnIdIsLessThanStopTxnId(startTxnId, this.stopTxnId);
        this.startTxnId = startTxnId;
    }

    public void setStopTxnId(long stopTxnId) {
        guaranteeStartTxnIdIsLessThanStopTxnId(this.startTxnId, stopTxnId);
        this.stopTxnId = stopTxnId;
    }

    @Nonnull
    @Override
    public Map<String, String> getConfiguration() {
        return Map.of("start-txn-id", Long.toString(startTxnId),
                    "stop-txn-id", Long.toString(stopTxnId));
    }

    @Nonnull
    @Override
    public IndexingStrategy.IndexingStrategyKey getIndexingStrategy() {
        return IndexingStrategy.IndexingStrategyKey.SINGLE_TXNS;
    }

    private static void guaranteeStartTxnIdIsLessThanStopTxnId(long startTxnId, long stopTxnId) {
        if (startTxnId > stopTxnId) {
            final String msg = String.format("Invalid configuration, startTxnId (%d) > stopId (%d)", startTxnId, stopTxnId);
            throw new IllegalArgumentException(msg);
        }
    }

}
