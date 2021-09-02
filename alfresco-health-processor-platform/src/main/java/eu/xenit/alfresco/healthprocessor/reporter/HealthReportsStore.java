package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HealthReportsStore {

    default void processReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        recordReportStats(pluginClass, reports);
        reports.forEach(report -> storeReport(pluginClass, report.withoutUnpersistableData()));
    }

    void storeReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report);

    void recordReportStats(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports);

    Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveStoredReports();

    Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrieveRecordedStats();

    void clear();

}
