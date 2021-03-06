package eu.xenit.alfresco.healthprocessor.indexing;

import lombok.Getter;

@Getter
public class IndexingConfiguration {

    private final IndexingStrategyKey indexingStrategy;

    private final long startTxnId;
    private final long stopTxnId;
    private final int txnBatchSize;

    public IndexingConfiguration(String indexingStrategyKey, long startTxnId, long stopTxnId, int txnBatchSize) {
        this(IndexingStrategyKey.fromKey(indexingStrategyKey), startTxnId, stopTxnId, txnBatchSize);
    }

    public IndexingConfiguration(IndexingStrategyKey indexingStrategy, long startTxnId, long stopTxnId,
            int txnBatchSize) {
        this.indexingStrategy = indexingStrategy;
        if (startTxnId > stopTxnId) {
            final String msg = "Invalid configuration, startTxnId (" + startTxnId + ") > stopId (" + stopTxnId + ")";
            throw new IllegalArgumentException(msg);
        }
        this.startTxnId = startTxnId;
        this.stopTxnId = stopTxnId;
        this.txnBatchSize = txnBatchSize;
    }

    public enum IndexingStrategyKey {
        TXNID("txn-id");

        @Getter
        private final String key;

        IndexingStrategyKey(String key) {
            this.key = key;
        }

        public static IndexingStrategyKey fromKey(String key) {
            for (IndexingStrategyKey s : IndexingStrategyKey.values()) {
                if (s.getKey().equals(key)) {
                    return s;
                }
            }
            return null;
        }


    }

}
