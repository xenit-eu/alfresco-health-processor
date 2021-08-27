package eu.xenit.alfresco.healthprocessor.processing;

import com.google.common.util.concurrent.RateLimiter;
import eu.xenit.alfresco.healthprocessor.fixer.NodeFixService;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.ReportsService;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
    private final NodeFixService fixService;

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
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
            updateCycleProgress();
            this.processNodeBatch(nodesToProcess);
            nodesToProcess = getNextNodesInTransaction();
        }
        updateCycleProgress();
    }

    private Set<NodeRef> getNextNodesInTransaction() {
        return transactionHelper.inNewTransaction(
                () -> indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize()), false);
    }

    private void processNodeBatch(Set<NodeRef> nodesToProcess) {
        ParameterCheck.mandatory("nodesToProcess", nodesToProcess);

        Set<NodeRef> copy = Collections.unmodifiableSet(nodesToProcess);
        for (HealthProcessorPlugin plugin : plugins) {
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

        Set<NodeHealthReport> pluginReports = transactionHelper
                .inNewTransaction(() -> Collections.unmodifiableSet(plugin.process(nodesToProcess)), configuration.isReadOnly());

        Set<NodeHealthReport> reports = validateNodeReports(nodesToProcess, pluginReports, plugin);

        Set<NodeHealthReport> healthAfterFixing = fixService.fixUnhealthyNodes(plugin, reports);

        transactionHelper.inNewTransaction(() -> reportsService.processReports(plugin.getClass(), healthAfterFixing),
                false);
    }

    private void updateCycleProgress() {
        transactionHelper.inNewTransaction(() -> reportsService.onProgress(indexingStrategy.getCycleProgress()), false);
    }

    private Set<NodeHealthReport> validateNodeReports(Set<NodeRef> nodesToProcess, Set<NodeHealthReport> reports, HealthProcessorPlugin plugin) {
        Set<NodeHealthReport> nodeHealthReports = reports;

        // Locate unreported nodes
        Set<NodeRef> reportedNodes = nodeHealthReports.stream().map(NodeHealthReport::getNodeRef).collect(Collectors.toSet());
        Set<NodeRef> unreportedNodes = new HashSet<>(nodesToProcess);
        unreportedNodes.removeAll(reportedNodes);

        // When we have unreported nodes, we have a problem: namely unreported nodes
        // When the number of reports does not match the number of requested nodes, we also have a problem:
        // * nodes are reported multiple times
        // * unrequested nodes are being reported as well
        if(!unreportedNodes.isEmpty() || nodeHealthReports.size() != nodesToProcess.size()) {
            log.warn("Plugin '{}' returned #{} reports for #{} nodes", plugin.getClass().getCanonicalName(), nodeHealthReports.size(), nodesToProcess.size());

            // Log about unreported nodes and insert health reports for them
            if(!unreportedNodes.isEmpty()) {
                log.warn("Plugin '{}' did not report for #{} nodes", plugin.getClass().getCanonicalName(), unreportedNodes.size());
                log.trace("Plugin '{}' did not report nodes: {}", plugin.getClass().getCanonicalName(), unreportedNodes);
                nodeHealthReports = new HashSet<>(nodeHealthReports); // We have to create a copy here, because reports is an unmodifiable Set
                nodeHealthReports.addAll(
                        unreportedNodes.stream()
                                .map(nodeRef -> new NodeHealthReport(NodeHealthStatus.UNREPORTED, nodeRef))
                                .collect(Collectors.toSet())
                );
                nodeHealthReports = Collections.unmodifiableSet(nodeHealthReports);
            }

            // Locate double reported nodes
            if(reportedNodes.size() != nodeHealthReports.size()) {
                Map<NodeRef, Set<NodeHealthReport>> reportedNodeCount = nodeHealthReports.stream()
                        .collect(Collectors.groupingBy(NodeHealthReport::getNodeRef, Collectors.toSet()));

                Map<NodeRef, Set<NodeHealthReport>> duplicateHealthReports = reportedNodeCount.entrySet().stream()
                        .filter(reportEntry -> reportEntry.getValue().size() > 1)
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                if(!duplicateHealthReports.isEmpty()) {
                    log.warn("Plugin '{}' generated duplicate reports for #{} nodes", plugin.getClass().getCanonicalName(), duplicateHealthReports.size());
                    log.trace("Plugin '{}' generated duplicate reports for nodes: {}", plugin.getClass().getCanonicalName(), duplicateHealthReports.keySet());
                }
            }

            // Locate illegally reported nodes
            Set<NodeRef> reportedNodesWithoutRequested = new HashSet<>(reportedNodes);
            reportedNodesWithoutRequested.removeAll(nodesToProcess);
            if(!reportedNodesWithoutRequested.isEmpty()) {
                log.warn("Plugin '{}' reported for #{} unrequested nodes", plugin.getClass().getCanonicalName(), reportedNodesWithoutRequested.size());
                log.trace("Plugin '{}' reported for unrequested nodes: {}", plugin.getClass().getCanonicalName(), reportedNodesWithoutRequested);
            }
        }
        return nodeHealthReports;
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
