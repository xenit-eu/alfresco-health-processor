package eu.xenit.alfresco.healthprocessor.reporter.store;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

public class InMemoryHealthReportsStore implements HealthReportsStore {

    private final Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> stats = new HashMap<>();
    private final Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> unhealthyReports = new HashMap<>();

    private final NodeHealthReportClassifier healthReportClassifier = new NodeHealthReportClassifier();

    @Override
    public void storeReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report) {
        if(!healthReportClassifier.shouldBeStored(report)) {
            return;
        }
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
    public void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {
        stats.clear();
        unhealthyReports.clear();
    }
}
