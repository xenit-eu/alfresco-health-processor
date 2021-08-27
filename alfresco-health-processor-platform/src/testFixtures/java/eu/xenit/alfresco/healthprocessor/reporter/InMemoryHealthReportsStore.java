package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryHealthReportsStore implements HealthReportsStore {

    private final Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> stats = new HashMap<>();
    private final Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> unhealthyReports = new HashMap<>();

    @Override
    public void storeUnhealthyReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report) {
        unhealthyReports.putIfAbsent(pluginClass, new ArrayList<>());
        unhealthyReports.get(pluginClass).add(report);
    }

    @Override
    public void recordReportStats(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        stats.putIfAbsent(pluginClass, new HashMap<>());
        reports.forEach(report -> stats.get(pluginClass).merge(report.getStatus(), 1L, Long::sum));
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveStoredReports() {
        return unhealthyReports;
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrieveRecordedStats() {
        return stats;
    }

    @Override
    public void clear() {
        stats.clear();
        unhealthyReports.clear();
    }
}
