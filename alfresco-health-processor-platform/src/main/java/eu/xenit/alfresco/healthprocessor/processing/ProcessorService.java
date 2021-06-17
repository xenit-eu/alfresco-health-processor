package eu.xenit.alfresco.healthprocessor.processing;

import com.google.common.util.concurrent.RateLimiter;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.ReportsService;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ParameterCheck;

@RequiredArgsConstructor
@Slf4j
public class ProcessorService {

    private final ProcessorConfiguration configuration;
    private final IndexingStrategy indexingStrategy;
    private final TransactionHelper transactionHelper;
    private final List<HealthProcessorPlugin> plugins;
    private final ReportsService reportsService;
    private final StateCache stateCache;

    @SuppressWarnings("UnstableApiUsage")
    private RateLimiter rateLimiter;

    public void execute() {
        if (hasNoEnabledPlugins()) {
            log.warn("Health-Processor scheduled but not a single enabled plugin found.");
            return;
        }

        try {
            transactionHelper.inNewTransaction(this::onStart, false);
            executeInternal();
            transactionHelper.inNewTransaction(this::onStop, false);
        } catch (Exception e) {
            log.error("Health-Processor: FAILED", e);
            transactionHelper.inNewTransaction(() -> onError(e), false);
            throw e;
        }
    }

    private void onStart() {
        log.info("Health-Processor: STARTING... Registered plugins: {}",
                plugins.stream().map(Object::getClass).map(Class::getSimpleName).collect(Collectors.toList()));

        stateCache.setState(ProcessorState.ACTIVE);

        indexingStrategy.onStart();
        reportsService.onStart();
        initializeRateLimiter();
    }

    private void onError(Exception e) {
        reportsService.onException(e);
        stateCache.setState(ProcessorState.FAILED);
    }

    private void onStop() {
        indexingStrategy.onStop();
        reportsService.onCycleDone();
        stateCache.setState(ProcessorState.IDLE);

        log.info("Health-Processor: DONE");
    }

    private void executeInternal() {
        Set<NodeRef> nodesToProcess = getNextNodesInTransaction();
        while (!nodesToProcess.isEmpty()) {
            this.processNodeBatch(nodesToProcess);
            nodesToProcess = getNextNodesInTransaction();
        }
    }

    private Set<NodeRef> getNextNodesInTransaction() {
        return transactionHelper.inNewTransaction(
                () -> indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize()), false);
    }

    private void processNodeBatch(Set<NodeRef> nodesToProcess) {
        ParameterCheck.mandatory("nodesToProcess", nodesToProcess);

        for (HealthProcessorPlugin plugin : plugins) {
            Set<NodeRef> copy = new HashSet<>(nodesToProcess);
            this.processNodeBatchRateLimited(copy, plugin);
        }
    }

    private void processNodeBatchRateLimited(Set<NodeRef> nodesToProcessCopy, HealthProcessorPlugin plugin) {
        if (rateLimiter != null) {
            log.debug("Trying to acquire rateLimiter...");
            // noinspection UnstableApiUsage
            rateLimiter.acquire();
        }
        this.processNodeBatchInTransaction(nodesToProcessCopy, plugin);
    }

    private void processNodeBatchInTransaction(Set<NodeRef> nodesToProcess, HealthProcessorPlugin plugin) {
        if (!plugin.isEnabled()) {
            log.debug("Plugin '{}' not enabled", plugin.getClass().getCanonicalName());
            return;
        }

        log.debug("Plugin '{}' will process #{} nodes", plugin.getClass().getCanonicalName(),
                nodesToProcess.size());

        Set<NodeHealthReport> reports = transactionHelper
                .inNewTransaction(() -> plugin.process(nodesToProcess), configuration.isReadOnly());
        transactionHelper.inNewTransaction(() -> reportsService.processReports(plugin.getClass(), reports), false);
    }

    private boolean hasNoEnabledPlugins() {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        return plugins.stream().noneMatch(HealthProcessorPlugin::isEnabled);
    }

    private void initializeRateLimiter() {
        // noinspection UnstableApiUsage
        this.rateLimiter = configuration.getMaxBatchesPerSecond() > 0 ?
                RateLimiter.create(configuration.getMaxBatchesPerSecond()) : null;
    }

    public ProcessorState getState() {
        return stateCache.getStateOrDefault();
    }
}
