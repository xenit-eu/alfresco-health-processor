package eu.xenit.alfresco.healthprocessor.reporter.log;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.ToggleableHealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.store.NodeHealthReportClassifier;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class StreamingLoggingHealthReporter extends ToggleableHealthReporter {

    private NodeHealthReportClassifier healthReportClassifier;

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {
        if (!log.isWarnEnabled()) {
            return;
        }
        Set<NodeHealthReport> interestingReports = reports.stream()
                .filter(healthReportClassifier::shouldBeStored)
                .collect(Collectors.toSet());

        if (!interestingReports.isEmpty()) {
            interestingReports.forEach(healthReport -> logReport(pluginClass, healthReport));
        }
    }

    private void logReport(Class<? extends HealthProcessorPlugin> pluginClass, NodeHealthReport healthReport) {
        log.warn("Plugin[{}]\t{} {}: {}", pluginClass.getSimpleName(), healthReport.getStatus(),
                healthReport.getNodeRef(), healthReport.getMessages());
        Set<NodeFixReport> fixReports = healthReport.data(NodeFixReport.class);
        for (NodeFixReport fixReport : fixReports) {
            if (fixReport.getFixStatus() != NodeFixStatus.SKIPPED) {
                log.info("\t\tFix {}: {}", fixReport.getFixStatus(), fixReport.getMessages());
            }
        }
    }
}
