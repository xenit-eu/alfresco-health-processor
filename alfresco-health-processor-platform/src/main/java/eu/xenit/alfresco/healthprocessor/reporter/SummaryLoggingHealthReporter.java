package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.SingleReportHealthReporter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SummaryLoggingHealthReporter extends SingleReportHealthReporter {

    Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> data = new HashMap<>();

    @Override
    public void onStart() {
        data.clear();
        log.info("Health-Processor: STARTING...");
    }

    @Override
    public void onStop() {
        data.forEach((key, value) -> log.info("Plugin[{}] generated reports: {}", key.getSimpleName(), value));
        log.info("Health-Processor: DONE");
    }

    @Override
    protected void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass) {
        data.putIfAbsent(pluginClass, new HashMap<>());
        data.get(pluginClass).merge(report.getStatus(), 1L, Long::sum);
    }
}
