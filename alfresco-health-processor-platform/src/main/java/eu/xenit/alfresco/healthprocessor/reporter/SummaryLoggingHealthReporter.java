package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.api.ToggleableHealthReporter;
import java.util.List;
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
    public void onCycleDone(List<ProcessorPluginOverview> overviews) {
        ParameterCheck.mandatory("overviews", overviews);
        
        log.info("Health-Processor done in {}", printDuration());
        logSummary(overviews);
        logUnhealthyNodes(overviews);
    }

    @Override
    public void onException(Exception e) {
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

    private void logUnhealthyNodes(List<ProcessorPluginOverview> overviews) {
        if (!log.isWarnEnabled()) {
            return;
        }

        log.warn("UNHEALTHY NODES ---");

        for (ProcessorPluginOverview overview : overviews) {
            List<NodeHealthReport> reports = overview.getReports();
            if (reports == null || reports.isEmpty()) {
                continue;
            }
            log.warn("Plugin[{}] (#{}): ", overview.getPluginClass().getSimpleName(), reports.size());
            reports.forEach(r -> log.warn("\t{}: {}", r.getNodeRef(), r.getMessages()));
        }

        log.warn(" --- ");

    }
}
