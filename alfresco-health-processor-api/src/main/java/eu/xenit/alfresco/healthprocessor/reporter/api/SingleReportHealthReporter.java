package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Health reporter that divides a batch into individual calls.
 *
 * Use this as your base class for simplified {@link HealthReporter} implementations that process reports one-by-one.
 */
public abstract class SingleReportHealthReporter extends ToggleableHealthReporter {

    protected static final Set<NodeHealthStatus> STATUSES_TO_HANDLE =
            new HashSet<>(Arrays.asList(NodeHealthStatus.values()));

    protected Set<NodeHealthStatus> statusesToHandle() {
        return STATUSES_TO_HANDLE;
    }

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {
        Set<NodeHealthStatus> statusesToHandle = statusesToHandle();

        for (NodeHealthReport report : reports) {
            if (statusesToHandle.contains(report.getStatus())) {
                processReport(report, pluginClass);
            }
        }

    }

    protected abstract void processReport(@Nonnull NodeHealthReport report,
            @Nonnull Class<? extends HealthProcessorPlugin> pluginClass);
}
