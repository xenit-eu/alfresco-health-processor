package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.api.ToggleableHealthReporter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.lang3.time.DurationFormatUtils;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class SummaryLoggingHealthReporter extends ToggleableHealthReporter {

    long startMs;

    @Override
    public void onStart() {
        startMs = System.currentTimeMillis();
    }

    @Override
    public void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {
        ParameterCheck.mandatory("overviews", overviews);
        
        log.info("Health-Processor done in {}", printDuration());
        logSummary(overviews);
        Arrays.stream(NodeHealthStatus.values())
                .filter(NodeHealthStatus::isInteresting)
                .forEachOrdered(status -> logNodesWithStatus(status, overviews));
    }

    @Override
    public void onException(@Nonnull Exception e) {
        log.warn("Health-Processor failed. Duration: {}, exception: {}", printDuration(), e.getMessage());
    }

    private String printDuration() {
        return DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startMs);
    }

    private void logSummary(List<ProcessorPluginOverview> overviews) {
        if (!log.isInfoEnabled()) {
            return;
        }

        log.info("SUMMARY ---");
        overviews.forEach(overview ->
                log.info("Plugin[{}] generated reports: {}",
                        overview.getPluginClass().getSimpleName(),
                        overview.getCountsByStatus()));
        log.info(" --- ");
    }

    private void logNodesWithStatus(NodeHealthStatus status, List<ProcessorPluginOverview> overviews) {
        if (!log.isWarnEnabled()) {
            return;
        }

        log.warn(status+" NODES ---");

        for (ProcessorPluginOverview overview : overviews) {
            List<NodeHealthReport> reports = overview.getReports().stream()
                    .filter(healthReport -> healthReport.getStatus() == status).collect(
                            Collectors.toList());
            if (reports.isEmpty()) {
                continue;
            }
            log.warn("Plugin[{}] (#{}): ", overview.getPluginClass().getSimpleName(), reports.size());
            reports.forEach(this::logReport);
        }

        log.warn(" --- ");

    }

    private void logReport(NodeHealthReport healthReport) {
        log.warn("\t{}: {}", healthReport.getNodeRef(), healthReport.getMessages());
        Set<NodeFixReport> fixReports = healthReport.data(NodeFixReport.class);
        for (NodeFixReport fixReport : fixReports) {
            if(fixReport.getFixStatus().isInteresting()) {
                log.info("\t\tFix {}: {}", fixReport.getFixStatus(), fixReport.getMessages());
            }
        }
    }
}
