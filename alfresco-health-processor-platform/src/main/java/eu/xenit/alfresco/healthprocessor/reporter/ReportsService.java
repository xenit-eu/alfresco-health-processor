package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.store.HealthReportsStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReportsService {

    private final HealthReportsStore reportsStore;
    private final List<HealthReporter> reporters;

    public void onStart() {
        forEachEnabledReporter(HealthReporter::onStart);
    }

    public void processReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        forEachEnabledReporter(reporter -> reporter.processReports(pluginClass, reports));
    }

    public void onException(Exception e) {
        forEachEnabledReporter(reporter -> reporter.onException(e));
    }

    public void onProgress(CycleProgress progress) {
        forEachEnabledReporter(reporter -> reporter.onProgress(progress));
    }

    public void onCycleDone() {
        Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> allReports =
                reportsStore.retrieveStoredReports();
        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> allStats =
                reportsStore.retrieveRecordedStats();

        Set<Class<? extends HealthProcessorPlugin>> pluginClasses = new HashSet<>();
        pluginClasses.addAll(allReports.keySet());
        pluginClasses.addAll(allStats.keySet());

        List<ProcessorPluginOverview> overviews = new ArrayList<>();
        pluginClasses.forEach(clazz -> {
            List<NodeHealthReport> reports = allReports.getOrDefault(clazz, Collections.emptyList());
            Map<NodeHealthStatus, Long> stats = allStats.getOrDefault(clazz, Collections.emptyMap());
            overviews.add(new ProcessorPluginOverview(clazz, stats, reports));
        });
        forEachEnabledReporter(reporter -> reporter.onCycleDone(overviews));
    }

    private void forEachEnabledReporter(Consumer<HealthReporter> consumer) {
        if (reporters == null || reporters.isEmpty()) {
            return;
        }
        reporters.stream()
                .filter(HealthReporter::isEnabled)
                .forEach(consumer);
    }

}
