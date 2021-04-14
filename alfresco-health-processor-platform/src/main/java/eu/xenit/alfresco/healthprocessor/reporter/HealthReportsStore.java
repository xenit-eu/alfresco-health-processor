package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HealthReportsStore {

    void storeReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports);

    Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveReports();

    Map<Class<? extends HealthProcessorPlugin>, EnumMap<NodeHealthStatus, Long>> retrieveReportStats();

    void clear();

}
