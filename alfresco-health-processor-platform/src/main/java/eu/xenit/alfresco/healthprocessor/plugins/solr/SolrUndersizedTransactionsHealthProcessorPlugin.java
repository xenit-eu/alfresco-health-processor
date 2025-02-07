package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.util.Pair;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin implements MeterBinder {

    public static final @NonNull String SELECTED_INDEXER_STRATEGY_PROPERTY = "eu.xenit.alfresco.healthprocessor.indexing.strategy";
    private static final @NonNull String MERGER_THREADS_CONFIGURATION_KEY = "merger-threads";
    private static final @NonNull String QUEUED_MERGE_REQUESTS_STATE_KEY = "queued-merge-requests";

    private final @NonNull TransactionHelper transactionHelper;
    private final @NonNull AtomicInteger queuedMergeRequests = new AtomicInteger(0);
    private final @NonNull ExecutorService mergerExecutor;
    private final @NonNull AbstractNodeDAOImpl nodeDAO;
    private final @Getter @NonNull Map<@NonNull String, @NonNull String> configuration;

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int mergerThreads,
                                                           @NonNull Properties properties,
                                                           @NonNull TransactionHelper transactionHelper,
                                                           @NonNull AbstractNodeDAOImpl nodeDAO) {
        // Used for testing purposes.
        this(enabled, mergerThreads, properties, transactionHelper, nodeDAO, null);
    }

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int mergerThreads,
                                                           @NonNull Properties properties,
                                                           @NonNull TransactionHelper transactionHelper,
                                                           @NonNull AbstractNodeDAOImpl nodeDAO,
                                                           @Nullable MeterRegistry meterRegistry) {
        super(enabled);
        if (enabled) guaranteeThresholdIndexerIsUsed(properties);

        this.transactionHelper = transactionHelper;
        this.mergerExecutor = Executors.newFixedThreadPool(mergerThreads);
        this.nodeDAO = nodeDAO;

        this.configuration = new HashMap<>(super.getConfiguration());
        this.configuration.put(MERGER_THREADS_CONFIGURATION_KEY, String.valueOf(mergerThreads));

        if (meterRegistry != null) bindTo(meterRegistry);
        else log.warn("The SolrUndersizedTransactionsHealthProcessorPlugin was not bound to a MeterRegistry. " +
                "This means that the queue size will not be reported.");
    }

    @Nonnull
    @Override
    protected Set<@NonNull NodeHealthReport> doProcess(Set<@NonNull NodeRef> allNodeRefs) {
        // This is NOT just a logging statement; the counter is incremented! Do not remove!
        log.debug("Queueing a new batch of ({}) transaction(s). Currently, there are ({}) merge requests queued.",
                allNodeRefs.size(), queuedMergeRequests.incrementAndGet());
        mergerExecutor.submit(() -> mergeTransactions(allNodeRefs));
        return NodeHealthReport.ofHealthy(allNodeRefs);
    }

    private void mergeTransactions(@NonNull Set<@NonNull NodeRef> backgroundWorkerBatch) {
        try {
            log.debug("Merging a new batch of ({}) node(s).", backgroundWorkerBatch.size());
            List<Long> nodeIds = backgroundWorkerBatch.parallelStream()
                    .map(this.nodeDAO::getNodePair)
                    .map(Pair::getFirst).collect(Collectors.toList());
            transactionHelper.inNewTransaction(() -> {
                    nodeDAO.touchNodes(nodeDAO.getCurrentTransactionId(true), nodeIds);
            }, false);
        } catch (Exception e) {
            log.error("An error occurred while merging a batch of ({}) node(s).", backgroundWorkerBatch.size(), e);
        } finally {
            queuedMergeRequests.decrementAndGet();
        }
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getState() {
        HashMap<String, String> returnValue = new HashMap<>(super.getState());
        returnValue.put(QUEUED_MERGE_REQUESTS_STATE_KEY, String.valueOf(queuedMergeRequests.get()));
        return returnValue;
    }

    private static void guaranteeThresholdIndexerIsUsed(@NonNull Properties properties) {
        String property = properties.getProperty(SELECTED_INDEXER_STRATEGY_PROPERTY);
        String expected = IndexingStrategy.IndexingStrategyKey.THRESHOLD.getKey();
        if (!(expected.equals(property)))
            throw new IllegalStateException(String.format("The SolrUndersizedTransactionsHealthProcessorPlugin can only be used with the (%s) indexing strategy. " +
                    "However, the (%s) strategy was selected. " +
                    "Please adjust the (%s) property.", expected, property, SELECTED_INDEXER_STRATEGY_PROPERTY));
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        registry.gauge("eu.xenit.alfresco.healthprocessor.plugin.solr-transaction-merger.merge-queue-size", queuedMergeRequests, AtomicInteger::get);
    }

}