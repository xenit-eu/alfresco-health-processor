package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryHealthReportsStore implements HealthReportsStore {

    private final Map<Class<? extends HealthProcessorPlugin>, EnumMap<NodeHealthStatus, Long>> data = new HashMap<>();
    private final Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> failures = new HashMap<>();

    @Override
    public void storeReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        data.putIfAbsent(pluginClass, new EnumMap<>(NodeHealthStatus.class));
        reports.forEach(report -> this.storeReport(pluginClass, report));
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrieveReports() {
        return failures;
    }

    @Override
    public Map<Class<? extends HealthProcessorPlugin>, EnumMap<NodeHealthStatus, Long>> retrieveReportStats() {
        return data;
    }

    @Override
    public void clear() {
        data.clear();
        failures.clear();
    }

    private void storeReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport report) {
        data.get(pluginClass).merge(report.getStatus(), 1L, Long::sum);

        if (NodeHealthStatus.UNHEALTHY.equals(report.getStatus())) {
            failures.putIfAbsent(pluginClass, new ArrayList<>());
            failures.get(pluginClass).add(report);
        }
    }
}
