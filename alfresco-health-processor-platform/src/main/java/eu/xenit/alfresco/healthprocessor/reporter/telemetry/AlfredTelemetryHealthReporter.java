package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.api.SingleReportHealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Description;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Key;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Tag;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
public class AlfredTelemetryHealthReporter extends SingleReportHealthReporter {

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final Set<Class<? extends HealthProcessorPlugin>> plugins = new HashSet<>();

    private final Map<ReportCounterKey, AtomicLong> reportCounters = new HashMap<>();

    private final AtomicReference<Float> progress = new AtomicReference<>(0f);

    private final MeterRegistry registry;

    public AlfredTelemetryHealthReporter() {
        this(Metrics.globalRegistry);
    }

    public AlfredTelemetryHealthReporter(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder(Key.ACTIVE, isActive, b -> b.get() ? 1d : 0d)
                .register(registry);
        Gauge.builder(Key.PLUGINS, plugins, Set::size)
                .description(Description.PLUGINS)
                .register(registry);
        Gauge.builder(Key.PROGRESS, progress, AtomicReference::get)
                .description(Description.PROGRESS)
                .register(registry);
    }

    @Override
    public void onStart() {
        isActive.set(true);
        progress.set(0f);
        resetCounters();
    }

    @Override
    public void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {
        isActive.set(false);
    }

    @Override
    public void onProgress(@Nonnull IndexingProgress progress) {
        this.progress.set(progress.getProgress());
    }

    @Override
    public void onException(@Nonnull Exception e) {
        isActive.set(false);
    }

    @Override
    protected void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass) {
        if (plugins.add(pluginClass)) {
            // First time that we see this plugin
            // Create counters for all possible statuses (metrics with tags that are only sometimes present mess with Prometheus)
            for (NodeHealthStatus healthStatus : NodeHealthStatus.values()) {
                reportCounters.computeIfAbsent(new ReportCounterKey(pluginClass, healthStatus), this::createCounter);
            }
        }
        reportCounters.get(new ReportCounterKey(pluginClass, report.getStatus())).incrementAndGet();
    }

    private void resetCounters() {
        reportCounters.forEach((key, value) -> value.set(0));
    }

    private AtomicLong createCounter(ReportCounterKey key) {
        AtomicLong counter = new AtomicLong();
        Gauge.builder(Key.REPORTS, counter, AtomicLong::get)
                .tag(Tag.PLUGIN, key.getPluginClass().getSimpleName())
                .tag(Tag.STATUS, key.getStatus().name())
                .register(registry);
        return counter;
    }

    @Value
    private static class ReportCounterKey {

        Class<? extends HealthProcessorPlugin> pluginClass;
        NodeHealthStatus status;

    }
}
