package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration.IndexingStrategyKey;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class IndexingStrategyView {

    String id;

    Map<String, String> state;
    Map<String, String> configuration;

    public IndexingStrategyView(IndexingConfiguration configuration, IndexingStrategy strategy) {
        this(
                configuration.getIndexingStrategy().getKey(),
                strategy.getState(),
                extractRelevantConfiguration(configuration)
        );
    }

    private static Map<String, String> extractRelevantConfiguration(IndexingConfiguration configuration) {
        Map<String, String> ret = new HashMap<>();

        if (IndexingStrategyKey.TXNID.equals(configuration.getIndexingStrategy())) {
            ret.put("start-txn-id", Long.toString(configuration.getStartTxnId()));
            ret.put("stop-txn-id", Long.toString(configuration.getStopTxnId()));
            ret.put("txn-batch-size", Integer.toString(configuration.getTxnBatchSize()));
        }

        return ret;
    }

}
