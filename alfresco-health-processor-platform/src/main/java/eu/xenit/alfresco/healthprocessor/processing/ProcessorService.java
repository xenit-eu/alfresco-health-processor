package eu.xenit.alfresco.healthprocessor.processing;

import com.google.common.util.concurrent.RateLimiter;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.AttributeHelper;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
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
    private final AttributeHelper attributeHelper;
    private final List<HealthProcessorPlugin> plugins;
    private final List<HealthReporter> reporters;

    private RateLimiter rateLimiter;

    public void execute() {
        if (hasNoEnabledPlugins()) {
            log.warn("Health-Processor scheduled but not a single enabled plugin found.");
            return;
        }

        try {
            onStart();
            executeInternal();
            onStop();
        } catch (Exception e) {
            onError(e);
            throw e;
        }
    }

    private void onStart() {
        log.info("Health-Processor: STARTING... Registered plugins: {}",
                plugins.stream().map(Object::getClass).map(Class::getSimpleName).collect(Collectors.toList()));

        attributeHelper.setAttribute(ProcessorState.ACTIVE, AttributeHelper.KEY_STATE);

        indexingStrategy.onStart();
        forEachEnabledReporter(HealthReporter::onStart);
        initializeRateLimiter();
    }

    private void onError(Exception e) {
        attributeHelper.setAttribute(ProcessorState.FAILED, AttributeHelper.KEY_STATE);
    }

    private void onStop() {
        indexingStrategy.onStop();
        forEachEnabledReporter(HealthReporter::onStop);
        attributeHelper.clearAttributes();

        log.info("Health-Processor: DONE");

    }

    private void executeInternal() {
        Set<NodeRef> nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        while (!nodesToProcess.isEmpty()) {
            this.processNodeBatch(nodesToProcess);
            nodesToProcess = indexingStrategy.getNextNodeIds(configuration.getNodeBatchSize());
        }
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
            rateLimiter.acquire();
        }
        transactionHelper.inNewTransaction(
                () -> this.processNodeBatchInTransaction(nodesToProcessCopy, plugin),
                configuration.isReadOnly()
        );
    }

    private void processNodeBatchInTransaction(Set<NodeRef> nodesToProcess, HealthProcessorPlugin plugin) {
        if (!plugin.isEnabled()) {
            log.debug("Plugin '{}' not enabled", plugin.getClass().getCanonicalName());
            return;
        }

        log.debug("Plugin '{}' will process #{} nodes", plugin.getClass().getCanonicalName(),
                nodesToProcess.size());

        Set<NodeHealthReport> reports = plugin.process(nodesToProcess);
        processReports(reports, plugin.getClass());
    }

    private void processReports(Set<NodeHealthReport> reports, Class<? extends HealthProcessorPlugin> pluginClass) {
        if (reports == null || reports.isEmpty()) {
            return;
        }

        forEachEnabledReporter(r -> {
            Set<NodeHealthReport> copy = new HashSet<>(reports);
            r.processReports(copy, pluginClass);
        });
    }

    private void forEachEnabledReporter(Consumer<HealthReporter> consumer) {
        if (reporters == null || reporters.isEmpty()) {
            return;
        }
        reporters.stream()
                .filter(HealthReporter::isEnabled)
                .forEach(consumer);
    }

    private boolean hasNoEnabledPlugins() {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        return plugins.stream().noneMatch(HealthProcessorPlugin::isEnabled);
    }

    private void initializeRateLimiter() {
        this.rateLimiter = configuration.getMaxBatchesPerSecond() > 0 ?
                RateLimiter.create(configuration.getMaxBatchesPerSecond()) : null;
    }

    public ProcessorState getState() {
        return attributeHelper.getAttributeOrDefault(AttributeHelper.KEY_STATE, ProcessorState.IDLE);
    }
}
