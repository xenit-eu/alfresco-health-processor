package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class SingleReportHealthReporter extends ToggleableHealthReporter {

    public static final Set<NodeHealthStatus> STATUSES_TO_HANDLE =
            new HashSet<>(Arrays.asList(NodeHealthStatus.values()));

    protected Set<NodeHealthStatus> statusesToHandle() {
        return STATUSES_TO_HANDLE;
    }

    @Override
    public void processReports(Set<NodeHealthReport> reports, Class<? extends HealthProcessorPlugin> pluginClass) {
        Set<NodeHealthStatus> statusesToHandle = statusesToHandle();

        for (NodeHealthReport report : reports) {
            if (statusesToHandle.contains(report.getStatus())) {
                processReport(report, pluginClass);
            }
        }

    }

    protected abstract void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass);
}
