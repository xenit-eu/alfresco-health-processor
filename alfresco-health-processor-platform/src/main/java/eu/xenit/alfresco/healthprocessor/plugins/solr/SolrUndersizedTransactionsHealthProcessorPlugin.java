package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.indexing.singletxns.SingleTransactionIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    public final static @NonNull QName DESCRIPTION_QNAME = QName.createQName("{http://www.alfresco.org/model/content/1.0}description");
    public final static @NonNull String DESCRIPTION_MESSAGE = "This node has been touched by the health processor to " +
            "trigger ACS to merge the transactions.";

    final static @NonNull Set<StoreRef> ARCHIVE_AND_WORKSPACE_STORE_REFS = Set.of(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

    private final @NonNull HashSet<@NonNull NodeRef> cache = new HashSet<>();
    private final int threshold;
    private final @NonNull TransactionHelper transactionHelper;
    private final @NonNull NodeService nodeService;
    private final @NonNull AtomicBoolean isRunning = new AtomicBoolean(false);

    public SolrUndersizedTransactionsHealthProcessorPlugin(@NonNull Properties properties, boolean enabled,
                                                           int threshold, @NonNull TransactionHelper transactionHelper,
                                                           @NonNull NodeService nodeService) {
        super(enabled);
        guaranteeSingleTransactionIndexerIsUsed(properties);

        this.threshold = threshold;
        this.transactionHelper = transactionHelper;
        this.nodeService = nodeService;

        SingleTransactionIndexingStrategy.listenToIndexerStart(this::onIndexerStart);
        SingleTransactionIndexingStrategy.listenToIndexerStop(this::onIndexerStop);
    }

    public void guaranteeSingleTransactionIndexerIsUsed(@NonNull Properties properties) throws AssertionError {
        if (SingleTransactionIndexingStrategy.isSelectedIndexingStrategy(properties)) return;

        throw new AssertionError("The SolrUndersizedTransactionsHealthProcessorPlugin has been activated, " +
                "which requires the SingleTransactionIndexingStrategy to be used. However, the latter one has not been " +
                "activated.");
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
        cache.clear(); // For the state.
    }

    @Nonnull
    @Override
    @Synchronized("cache")
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> allNodeRefs) {
        // Ignore the non-archive and non-workspace nodes. Just make sure their health is also reported at the end.
        Set<NodeRef> filteredNodeRefs = filterWorkspaceAndArchiveNodes(allNodeRefs);

        // For the relevant nodes from the current transaction:
        // A) if nothing from the previous transactions has been cached yet & the current transaction is large enough,
        //      then nothing needs to be merged. Do nothing in that case.
        if (cache.isEmpty() && filteredNodeRefs.size() >= threshold) {
            log.trace("The current transaction is large enough; received {} node(s) while the threshold value is ({}). " +
                    "No transactions will be merged.", filteredNodeRefs.size(), threshold);
        } else {
            // B) if A) does not apply, add the current transaction to the cache. Once the cache overflows,
            //      start merging the transactions.
            cache.addAll(filteredNodeRefs);
            log.trace("Added {} nodes(s) to the cache (current size: {}).", filteredNodeRefs.size(), cache.size());

            if (cache.size() >= threshold) {
                mergeTransactions();
                cache.clear();
            }
        }

        return NodeHealthReport.ofHealthy(allNodeRefs);
    }

    private void mergeTransactions() {
        log.debug("The cache (current size: {}) has reached the threshold value ({}). Merging the transactions.",
                cache.size(), threshold);

        AuthenticationUtil.runAsSystem(() -> {
           transactionHelper.inNewTransaction(() -> {
               for (NodeRef nodeRef : cache) {
                    nodeService.setProperty(nodeRef, DESCRIPTION_QNAME, DESCRIPTION_MESSAGE);
               }
           }, false);

           return null;
        });
    }

    private @NonNull Set<NodeRef> filterWorkspaceAndArchiveNodes(@NonNull Set<NodeRef> allNodeRefs) {
        return allNodeRefs.stream()
                .filter(nodeRef -> ARCHIVE_AND_WORKSPACE_STORE_REFS.contains(nodeRef.getStoreRef()))
                .collect(Collectors.toSet());
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

}
