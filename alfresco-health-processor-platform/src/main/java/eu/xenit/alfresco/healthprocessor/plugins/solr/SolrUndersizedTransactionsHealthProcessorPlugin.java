package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    public final static @NonNull String SELECTED_INDEXER_STRATEGY_PROPERTY = "eu.xenit.alfresco.healthprocessor.indexing.strategy";

    private final @NonNull TransactionHelper transactionHelper;
    private final @NonNull AtomicInteger queuedMergeRequests = new AtomicInteger(0);
    private final @NonNull ExecutorService mergerExecutor;
    private final @NonNull AbstractNodeDAOImpl nodeDAO;
    private final @Getter @NonNull Map<String, String> configuration;

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int mergerThreads,
                                                           @NonNull Properties properties,
                                                           @NonNull TransactionHelper transactionHelper,
                                                           @NonNull AbstractNodeDAOImpl nodeDAO) {
        super(enabled);
        if (enabled) guaranteeThresholdIndexerIsUsed(properties);

        this.transactionHelper = transactionHelper;
        this.mergerExecutor = Executors.newFixedThreadPool(mergerThreads);
        this.nodeDAO = nodeDAO;

        this.configuration = new HashMap<>(super.getConfiguration());
        this.configuration.put("merger-threads", String.valueOf(mergerThreads));
    }

    @Nonnull
    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> allNodeRefs) {
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
    public Map<String, String> getState() {
        HashMap<String, String> returnValue = new HashMap<>(super.getState());
        returnValue.put("queued-merge-requests", String.valueOf(queuedMergeRequests.get()));
        return returnValue;
    }

    private static void guaranteeThresholdIndexerIsUsed(@NonNull Properties properties) {
        String property = properties.getProperty(SELECTED_INDEXER_STRATEGY_PROPERTY);
        String expected = IndexingStrategy.IndexingStrategyKey.THRESHOLD.getKey();
        if (!(expected.equals(property)))
            throw new RuntimeException(String.format("The SolrUndersizedTransactionsHealthProcessorPlugin can only be used with the (%s) indexing strategy. " +
                    "However, the (%s) strategy was selected. " +
                    "Please adjust the (%s) property.", expected, property, SELECTED_INDEXER_STRATEGY_PROPERTY));
    }

}