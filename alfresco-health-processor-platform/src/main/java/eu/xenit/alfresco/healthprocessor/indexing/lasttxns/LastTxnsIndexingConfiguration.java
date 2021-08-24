package eu.xenit.alfresco.healthprocessor.indexing.lasttxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy.IndexingStrategyKey;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class LastTxnsIndexingConfiguration implements IndexingConfiguration {
    long lookbackTransactions;
    long batchSize;

    @Nonnull
    @Override
    public IndexingStrategyKey getIndexingStrategy() {
        return IndexingStrategyKey.LAST_TXNS;
    }

    @Nonnull
    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> ret = new HashMap<>();
        ret.put("number-of-transactions", Long.toString(lookbackTransactions));
        ret.put("txn-batch-size", Long.toString(batchSize));
        return ret;
    }
}
