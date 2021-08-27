package eu.xenit.alfresco.healthprocessor.indexing.txnid;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy.IndexingStrategyKey;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class TxnIdIndexingConfiguration implements IndexingConfiguration {
    long startTxnId;
    long stopTxnId;
    int txnBatchSize;

    public TxnIdIndexingConfiguration(long startTxnId, long stopTxnId, int txnBatchSize) {
        if (startTxnId > stopTxnId) {
            final String msg = "Invalid configuration, startTxnId (" + startTxnId + ") > stopId (" + stopTxnId + ")";
            throw new IllegalArgumentException(msg);
        }
        this.startTxnId = startTxnId;
        this.stopTxnId = stopTxnId;
        this.txnBatchSize = txnBatchSize;
    }

    @Nonnull
    @Override
    public IndexingStrategyKey getIndexingStrategy() {
        return IndexingStrategyKey.TXNID;
    }

    @Nonnull
    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> ret = new HashMap<>();
        ret.put("start-txn-id", Long.toString(getStartTxnId()));
        ret.put("stop-txn-id", Long.toString(getStopTxnId()));
        ret.put("txn-batch-size", Integer.toString(getTxnBatchSize()));
        return ret;
    }
}
