package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdIndexingConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexingConfigUtil {

    public static TxnIdIndexingConfiguration defaultConfig() {
        return config(-1L, Long.MAX_VALUE, 1000);
    }

    public static TxnIdIndexingConfiguration config(long startTxn, long stopTxn, int txnBatchSize) {
        return new TxnIdIndexingConfiguration(startTxn, stopTxn, txnBatchSize);
    }
}
