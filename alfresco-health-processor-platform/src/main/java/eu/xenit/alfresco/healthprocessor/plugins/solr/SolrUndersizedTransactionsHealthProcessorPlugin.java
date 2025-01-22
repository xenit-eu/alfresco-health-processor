package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.indexing.singletxns.SingleTransactionIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final static @NonNull Set<StoreRef> ARCHIVE_AND_WORKSPACE_STORE_REFS = Set.of(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
    private final static @NonNull String UNHEALTHY_MESSAGE = "Node was collected from transactions that were too small to meet the threshold value";

    private final @NonNull HashSet<@NonNull NodeRef> cache = new HashSet<>();
    private final int threshold;
    private final @NonNull AtomicBoolean isRunning = new AtomicBoolean(false);

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int threshold) {
        super(enabled);

        this.threshold = threshold;
        // TODO: check if the correct indexer has been used.
        SingleTransactionIndexingStrategy.listenToIndexerStart(this::onIndexerStart);
        SingleTransactionIndexingStrategy.listenToIndexerStop(this::onIndexerStop);
    }

    @Synchronized("cache")
    private void onIndexerStart() {
        log.debug("Processing the start event from the single-transaction indexing strategy.");
        isRunning.set(true);
        cache.clear();
    }

    @Synchronized("cache")
    private void onIndexerStop() {
        log.debug("Processing the stop event from the single-transaction indexing strategy.");
        isRunning.set(false);
        cache.clear();
    }

    @Nonnull
    @Override
    @Synchronized("cache")
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        // This health processor plugin is only interested in the nodes from the archive & workspace store, so we filter here.
        // TODO: this is wrong. Alfresco starts complaining about non-reported nodes.
        nodeRefs = filterArchiveAndWorkspaceNodeRefs(nodeRefs);

        // If nothing is cached yet, and the batch size is sufficiently large, we don't need to merge the transactions.
        if (cache.isEmpty() && nodeRefs.size() >= threshold) {
            log.debug("The size of the received batch ({}) is larger than the threshold value ({}); " +
                    "reporting the nodes as healthy.", nodeRefs.size(), threshold);
            return NodeHealthReport.ofHealthy(nodeRefs);
        }

        // Add the nodes to the cache. If the cache becomes to big, report the nodes as unhealthy so that they can be merged.
        cache.addAll(nodeRefs);
        if (cache.size() >= threshold) {
            log.debug("The size of the cache ({}) is now larger than the threshold value ({}); " +
                    "reporting the nodes as unhealthy.", cache.size(), threshold);
            Set<NodeHealthReport> returnValue = NodeHealthReport.ofUnhealthy(cache, UNHEALTHY_MESSAGE);
            cache.clear();
            return returnValue;
        }

        // Keep increasing the cache size.
        log.trace("The size of the cache ({}) is still smaller than the threshold value ({}); " +
                "waiting for more nodes to be processed.", cache.size(), threshold);
        return Set.of();

    }

    @Override
    @Synchronized("cache")
    public Map<String, String> getState() {
        return Map.of("isRunning", Boolean.toString(isRunning.get()),
                "cacheSize", Integer.toString(cache.size()));
    }

    @Override
    public Map<String, String> getConfiguration() {
        HashMap<String, String> returnValue = new HashMap<>(super.getConfiguration());
        returnValue.put("threshold", Integer.toString(threshold));
        return returnValue;
    }

    private static @NonNull Set<NodeRef> filterArchiveAndWorkspaceNodeRefs(@NonNull Collection<NodeRef> nodeRefs) {
        return nodeRefs.stream()
                .filter(nodeRef -> ARCHIVE_AND_WORKSPACE_STORE_REFS.contains(nodeRef.getStoreRef()))
                .collect(Collectors.toSet());
    }
}
