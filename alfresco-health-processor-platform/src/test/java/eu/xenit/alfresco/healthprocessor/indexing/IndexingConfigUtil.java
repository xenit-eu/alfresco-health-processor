package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration.IndexingStrategyKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexingConfigUtil {

    public static IndexingConfiguration defaultConfig() {
        return config(-1L, Long.MAX_VALUE, 1000);
    }

    public static IndexingConfiguration config(long startTxn, long stopTxn, int txnBatchSize) {
        return new IndexingConfiguration(IndexingStrategyKey.TXNID, startTxn, stopTxn, txnBatchSize);
    }
}
