package eu.xenit.alfresco.healthprocessor.reporter.log;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
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

        boolean loggedStart = false;

        for (ProcessorPluginOverview overview : overviews) {
            List<NodeHealthReport> reports = overview.getReports().stream()
                    .filter(healthReport -> healthReport.getStatus() == status).collect(
                            Collectors.toList());
            if (reports.isEmpty()) {
                continue;
            }
            if (!loggedStart) {
                log.warn(status + " NODES ---");
                loggedStart = true;
            }
            long countedReports = overview.getCountsByStatus().getOrDefault(status, 0L);
            long receivedReports = reports.size();
            log.warn("Plugin[{}] (#{}): ", overview.getPluginClass().getSimpleName(), receivedReports);
            reports.forEach(this::logReport);
            if(countedReports > receivedReports) {
                log.warn("\t... and {} additional reports that are not logged.", countedReports - receivedReports);
            }
        }
        if (loggedStart) {
            log.warn(" --- ");
        }

    }

    private void logReport(NodeHealthReport healthReport) {
        log.warn("\t{}: {}", healthReport.getNodeRef(), healthReport.getMessages());
        Set<NodeFixReport> fixReports = healthReport.data(NodeFixReport.class);
        for (NodeFixReport fixReport : fixReports) {
            if (fixReport.getFixStatus() != NodeFixStatus.SKIPPED) {
                log.info("\t\tFix {}: {}", fixReport.getFixStatus(), fixReport.getMessages());
            }
        }
    }
}
