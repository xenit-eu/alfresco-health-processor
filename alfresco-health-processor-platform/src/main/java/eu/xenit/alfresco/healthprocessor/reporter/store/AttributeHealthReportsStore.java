package eu.xenit.alfresco.healthprocessor.reporter.store;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.util.Pair;

/**
 * {@link HealthReportsStore} implementation that persists stats and failing reports via the {@link
 * org.alfresco.service.cmr.attributes.AttributeService}. Reports and stats are stored using the following key value
 * combination:
 * <ul>
 *     <li>health-processor.reports.${random-uuid}=${Pair(PluginClass, NodeHealthReport)}</li>
 *     <li>health-processor.report-stats.${PluginClass}=${HashMap(NodeHealthStatus, Count)}</li>
 * </ul>
 */
@RequiredArgsConstructor
@Slf4j
public class AttributeHealthReportsStore implements HealthReportsStore {

    public static final String ATTR_KEY_REPORTS = "reports";
    public static final String ATTR_KEY_REPORT_STATS = "report-stats";
    public static final String ATTR_KEY_STORED_REPORTS_COUNT = "stored-reports-count";

    private final AttributeStore attributeStore;
    private final NodeHealthReportClassifier healthReportClassifier;
    private final long maxStoredReports;
    private AtomicLong storedReportsCounter;

    @Override
    public void onStart() {
        Long storedReportsCount = attributeStore.getAttributeOrDefault(ATTR_KEY_STORED_REPORTS_COUNT, 0L);
        storedReportsCounter = new AtomicLong(storedReportsCount);
    }

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {
        HealthReportsStore.super.processReports(pluginClass, reports);
        attributeStore.setAttribute(storedReportsCounter.longValue(), ATTR_KEY_STORED_REPORTS_COUNT);
    }

    @Override
    public void storeReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report) {
        // There is no need to store the report if it does not have to be sent to the reporters
        if (!healthReportClassifier.shouldBeStored(report)) {
            return;
        }
        long storedReportsCount = storedReportsCounter.getAndIncrement();
        if(storedReportsCount < maxStoredReports) {
            Pair<Class<? extends HealthProcessorPlugin>, NodeHealthReport> pluginClassWithReport =
                    new Pair<>(pluginClass, report);
            attributeStore.setAttribute(pluginClassWithReport, ATTR_KEY_REPORTS, UUID.randomUUID().toString());
        } else if(storedReportsCount == maxStoredReports) {
            // Only warn when we just go over the limit
            // So we don't trigger the next warning for all future report stores
            log.warn("Received too many reports to store (max={}). All future reports in this cycle will be dropped.",
                    storedReportsCount);
        }
    }

    @Override
    public void recordReportStats(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        Map<NodeHealthStatus, Long> oldStats = attributeStore.getAttribute(ATTR_KEY_REPORT_STATS, pluginClass);
        if (oldStats == null) {
            oldStats = new EnumMap<>(NodeHealthStatus.class);
        }
        for (NodeHealthReport report : reports) {
            oldStats.compute(report.getStatus(), (key, oldValue) -> oldValue == null ? 1L : oldValue + 1L);
        }

        attributeStore.setAttribute((Serializable) oldStats, ATTR_KEY_REPORT_STATS, pluginClass);
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveStoredReports() {
        Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> ret = new HashMap<>();

        Map<Serializable, Serializable> attributes = attributeStore.getAttributes(ATTR_KEY_REPORTS);
        attributes.forEach((key, value) -> {
            Pair<Class<? extends HealthProcessorPlugin>, NodeHealthReport> pluginClassWithReport =
                    (Pair<Class<? extends HealthProcessorPlugin>, NodeHealthReport>) value;
            ret.putIfAbsent(pluginClassWithReport.getFirst(), new ArrayList<>());
            ret.get(pluginClassWithReport.getFirst()).add(pluginClassWithReport.getSecond());
        });

        return ret;
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrieveRecordedStats() {
        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> ret = new HashMap<>();

        Map<Serializable, Serializable> attributes = attributeStore.getAttributes(ATTR_KEY_REPORT_STATS);
        attributes.forEach((key, value) -> {
            Class<? extends HealthProcessorPlugin> pluginClass = (Class<? extends HealthProcessorPlugin>) key;
            Map<NodeHealthStatus, Long> stats = (Map<NodeHealthStatus, Long>) value;
            ret.put(pluginClass, stats);
        });

        return ret;
    }

    @Override
    public void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {
        attributeStore.removeAttributes(ATTR_KEY_REPORTS);
        attributeStore.removeAttributes(ATTR_KEY_REPORT_STATS);
        attributeStore.removeAttributes(ATTR_KEY_STORED_REPORTS_COUNT);
    }

    @Override
    public Map<String, String> getConfiguration() {
        return Collections.singletonMap("max-stored-reports", Objects.toString(maxStoredReports));
    }

    @Override
    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<>();
        state.put("storedReportsCounter", Objects.toString(storedReportsCounter.longValue()));
        return state;
    }
}
