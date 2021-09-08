package eu.xenit.alfresco.healthprocessor.fixer;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nonnull;

public class ReportEmittingHealthFixerPlugin implements HealthFixerPlugin {

    private final Queue<Set<NodeFixReport>> fixReports = new LinkedList<>();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Nonnull
    @Override
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass,
            Set<NodeHealthReport> unhealthyReports) {
        Set<NodeFixReport> reports = fixReports.poll();
        if (reports == null) {
            return Collections.emptySet();
        }
        return reports;
    }

    public void scheduleFixReports(Set<NodeFixReport> fixReports) {
        this.fixReports.offer(fixReports);
    }

    public void scheduleFixReports(NodeFixReport... fixReports) {
        scheduleFixReports(new HashSet<>(Arrays.asList(fixReports)));
    }
}
