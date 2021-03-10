package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.SingleReportHealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Description;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Key;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Tag;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Value;

public class AlfredTelemetryHealthReporter extends SingleReportHealthReporter {

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final Set<Class<? extends HealthProcessorPlugin>> plugins = new HashSet<>();

    private final Map<ReportCounterKey, Counter> reportCounters = new HashMap<>();

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
    }

    @Override
    public void onStart() {
        isActive.set(true);
    }

    @Override
    public void onStop() {
        isActive.set(false);
    }

    @Override
    protected void processReport(NodeHealthReport report, Class<? extends HealthProcessorPlugin> pluginClass) {
        plugins.add(pluginClass);
        reportCounters.computeIfAbsent(new ReportCounterKey(pluginClass, report.getStatus()), this::createCounter)
                .increment();
    }

    private Counter createCounter(ReportCounterKey key) {
        return Counter.builder(Key.REPORTS)
                .tag(Tag.PLUGIN, key.getPluginClass().getSimpleName())
                .tag(Tag.STATUS, key.getStatus().name())
                .register(registry);
    }

    @Value
    private static class ReportCounterKey {

        Class<? extends HealthProcessorPlugin> pluginClass;
        NodeHealthStatus status;

    }
}
