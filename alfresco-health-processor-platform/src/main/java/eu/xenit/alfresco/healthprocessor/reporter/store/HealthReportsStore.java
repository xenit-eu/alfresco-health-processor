package eu.xenit.alfresco.healthprocessor.reporter.store;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

public interface HealthReportsStore extends HealthReporter {
    @Override
    default boolean isEnabled() {
        return true;
    }

    @Override
    default void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass, @Nonnull Set<NodeHealthReport> reports) {
        recordReportStats(pluginClass, reports);
        reports.forEach(report -> storeReport(pluginClass, report.withoutUnpersistableData()));
    }

    void storeReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report);

    void recordReportStats(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports);

    Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveStoredReports();

    Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrieveRecordedStats();

}
