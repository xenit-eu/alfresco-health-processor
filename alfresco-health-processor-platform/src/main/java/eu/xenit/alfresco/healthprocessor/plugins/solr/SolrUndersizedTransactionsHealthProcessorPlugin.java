package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;


public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final static @NonNull Logger logger = LoggerFactory.getLogger(SolrUndersizedTransactionsHealthProcessorPlugin.class);

    private final static @NonNull String THRESHOLD_PARAMETER_NAME = "eu.xenit.alfresco.healthprocessor.plugin.merger.threshold";
    private final static int DEFAULT_THRESHOLD = 1000;

    private final @NonNull HashSet<NodeRef> rememberedNodes = new HashSet<>();
    private final int threshold;
    private long lastProcessedTransactionId = Long.MIN_VALUE;
    private final @NonNull AttributeStore attributeStore;

    public SolrUndersizedTransactionsHealthProcessorPlugin(@NonNull Properties globalProperties, @NonNull AttributeStore attributeStore) throws IllegalStateException {
        // TODO: double-check this part.
        String[] properties = {"eu.xenit.alfresco.healthprocessor.indexing.strategy", "eu.xenit.alfresco.healthprocessor.indexing.txn-id.txn-batch-size"};
        String[] requiredValue = {"txn-id", "1"}; // Force the indexer to go over all transactions (offset allowed) one at a time.
        for (int i = 0; i < properties.length; i++) {
            if (!globalProperties.containsKey(properties[i]) || !requiredValue[i].equals(globalProperties.get(properties[i]))) {
                throw new IllegalStateException(String.format("The SolrUndersizedTransactionsHealthProcessorPlugin has been enabled, " +
                        "which requires the (%s) property to be set to (%s), but was (%s)", properties[i], requiredValue[i],
                        globalProperties.get(properties[i])));
            }
        }

        this.attributeStore = attributeStore;
        this.threshold = Integer.parseInt(globalProperties.getProperty(THRESHOLD_PARAMETER_NAME, String.valueOf(DEFAULT_THRESHOLD)));
        logger.info("SolrUndersizedTransactionsHealthProcessorPlugin has been enabled with a threshold value of ({}).", threshold);
    }

    @Nonnull
    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        updateState();

        // Did we just start a new batch, and is the current transaction already sufficiently large?
        if (rememberedNodes.isEmpty() && nodeRefs.size() >= threshold) {
            logger.debug("The number of nodes in the current transaction ({}) surpasses the threshold ({}) value; " +
                    "skipping the current transaction.", nodeRefs.size(), threshold);
            return NodeHealthReport.ofHealthy(nodeRefs);
        }

        // We're currently in a batch & the size of that batch is not sufficiently large yet.
        rememberedNodes.addAll(nodeRefs);
        if (rememberedNodes.size() >= threshold) {
            Set<NodeHealthReport> returnValue = NodeHealthReport.ofUnhealthy(rememberedNodes);
            rememberedNodes.clear();

            logger.debug("Currently keeping track of ({}) nodes, which surpasses the threshold ({}) value. " +
                    "Marking the nodes as unhealthy.", returnValue.size(), threshold);
            return returnValue;
        }

        // Still not large enough.
        return Set.of();
    }

    private void updateState() {
        if (indexerHasRestarted()) {
            rememberedNodes.clear();
            logger.debug("The indexer has restarted, clearing the remembered nodes.");
        }
        lastProcessedTransactionId = getCurrentlyIndexedTransactionId();
    }

    private long getCurrentlyIndexedTransactionId() {
        return attributeStore.getAttribute("blahbalh");
    }

    private boolean indexerHasRestarted() {
        long currentTransaction = getCurrentlyIndexedTransactionId();
        return lastProcessedTransactionId >= currentTransaction;
    }

    @Override
    public Map<String, String> getConfiguration() {
        HashMap<String, String> returnValue = new HashMap<>(super.getConfiguration());
        returnValue.put("threshold", String.valueOf(threshold));
        return returnValue;
    }
}
