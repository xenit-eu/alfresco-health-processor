package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.SingleReportHealthReporter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class SummaryLoggingHealthReporter extends SingleReportHealthReporter {

    Map<Class<? extends HealthProcessorPlugin>, EnumMap<NodeHealthStatus, Long>> data = new HashMap<>();
    Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> failures = new HashMap<>();

    long startMs;

    @Override
    public void onStart() {
        startMs = System.currentTimeMillis();
    }

    @Override
    public void onStop() {
        if (log.isInfoEnabled()) {
            long elapsedMs = System.currentTimeMillis() - startMs;
            log.info("Health-Processor done in {}", DurationFormatUtils.formatDurationHMS(elapsedMs));
        }
        logSummary();
        logUnhealthyNodes();
        reset();
    }

    @Override
    protected void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass) {
        data.putIfAbsent(pluginClass, new EnumMap<>(NodeHealthStatus.class));
        data.get(pluginClass).merge(report.getStatus(), 1L, Long::sum);

        if (NodeHealthStatus.UNHEALTHY.equals(report.getStatus())) {
            failures.putIfAbsent(pluginClass, new ArrayList<>());
            failures.get(pluginClass).add(report);
        }
    }

    private void logSummary() {
        if (!log.isInfoEnabled()) {
            return;
        }
        log.info("SUMMARY ---");
        data.forEach((key, value) -> log.info("Plugin[{}] generated reports: {}", key.getSimpleName(), value));
        log.info(" --- ");
    }

    private void logUnhealthyNodes() {
        if (failures.isEmpty() || !log.isWarnEnabled()) {
            return;
        }

        log.warn("UNHEALTHY NODES ---");

        failures.forEach((clazz, reports) -> {
            log.warn("Plugin[{}] (#{}): ", clazz.getSimpleName(), reports.size());
            reports.forEach(r -> log.warn("\t{}: {}", r.getNodeRef(), r.getMessages()));
        });

        log.warn(" --- ");

    }

    private void reset() {
        data.clear();
        failures.clear();
    }
}
