package eu.xenit.alfresco.processor.indexing;

import eu.xenit.alfresco.processor.indexing.IndexingConfiguration.IndexingStrategyKey;

public class IndexingConfigUtil {

    private IndexingConfigUtil() {
        // private ctor to hide implicit public one
    }

    public static IndexingConfiguration defaultConfig() {
        return config(-1L, Long.MAX_VALUE, 1000);
    }

    public static IndexingConfiguration config(long startTxn, long stopTxn, int txnBatchSize) {
        return new IndexingConfiguration(IndexingStrategyKey.TXNID, startTxn, stopTxn, txnBatchSize);
    }
}
