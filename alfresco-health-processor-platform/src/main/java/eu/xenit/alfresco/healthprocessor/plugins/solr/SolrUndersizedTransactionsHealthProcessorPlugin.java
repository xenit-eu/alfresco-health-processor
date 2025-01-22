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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final static @NonNull Set<StoreRef> ARCHIVE_AND_WORKSPACE_STORE_REFS = Set.of(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

    private final @NonNull HashSet<@NonNull NodeRef> cache = new HashSet<>();
    private final int threshold;

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int threshold) {
        super(enabled);

        this.threshold = threshold;
        // TODO: check if the correct indexer has been used.
        SingleTransactionIndexingStrategy.listenToIndexerStart(this::onIndexerRestart);
    }

    @Synchronized("cache")
    private void onIndexerRestart() {
        log.debug("Processing the start event from the single-transaction indexing strategy.");
        cache.clear();
    }

    @Nonnull
    @Override
    @Synchronized("cache")
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        // This health processor plugin is only interested in the nodes from the archive & workspace store, so we filter here.
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
            Set<NodeHealthReport> returnValue = NodeHealthReport.ofUnhealthy(cache);
            cache.clear();
            return returnValue;
        }

        // Keep increasing the cache size.
        log.trace("The size of the cache ({}) is still smaller than the threshold value ({}); " +
                "waiting for more nodes to be processed.", cache.size(), threshold);
        return Set.of();

    }

    @Override
    public Map<String, String> getState() {
        return super.getState(); // TODO.
    }

    @Override
    public Map<String, String> getConfiguration() {
        return super.getConfiguration(); // TODO.
    }

    private static @NonNull Set<NodeRef> filterArchiveAndWorkspaceNodeRefs(@NonNull Collection<NodeRef> nodeRefs) {
        return nodeRefs.stream()
                .filter(nodeRef -> ARCHIVE_AND_WORKSPACE_STORE_REFS.contains(nodeRef.getStoreRef()))
                .collect(Collectors.toSet());
    }
}
