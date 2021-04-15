package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.alfresco.util.Pair;

/*
 * health-processor.reports.${random-uuid}=${Pair<PluginClass, NodeHealthReport>}
 * health-processor.report-stats.${PluginClass}=${EnumMap<Status, Count>}
 */
@RequiredArgsConstructor
public class AttributeHealthReportsStore implements HealthReportsStore {

    private static final String ATTR_KEY_REPORTS = "reports";
    private static final String ATTR_KEY_REPORT_STATS = "report-stats";

    private final AttributeStore attributeStore;

    @Override
    public void storeUnhealthyReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report) {
        Pair<Class<? extends HealthProcessorPlugin>, NodeHealthReport> pluginClassWithReport =
                new Pair<>(pluginClass, report);
        attributeStore.setAttribute(pluginClassWithReport, ATTR_KEY_REPORTS, UUID.randomUUID().toString());
    }

    @Override
    public void recordReportStats(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        HashMap<NodeHealthStatus, Long> oldStats = attributeStore.getAttribute(ATTR_KEY_REPORT_STATS, pluginClass);
        if (oldStats == null) {
            oldStats = new HashMap<>();
        }
        for (NodeHealthReport report : reports) {
            oldStats.compute(report.getStatus(), (key, oldValue) -> oldValue == null ? 1L : oldValue + 1L);
        }

        attributeStore.setAttribute(oldStats, ATTR_KEY_REPORT_STATS, pluginClass);
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
    public void clear() {
        attributeStore.removeAttributes(ATTR_KEY_REPORTS);
        attributeStore.removeAttributes(ATTR_KEY_REPORT_STATS);
    }

    @Value
    private static class NodeHealthReportPerPlugin {

        Class<? extends NodeHealthReport> pluginClass;
        NodeHealthReport report;

    }
}
