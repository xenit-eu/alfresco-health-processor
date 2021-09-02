package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.NoOpHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Key;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Tag;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

class AlfredTelemetryHealthReporterTest {

    private SimpleMeterRegistry meterRegistry;
    AlfredTelemetryHealthReporter reporter;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        reporter = new AlfredTelemetryHealthReporter(meterRegistry);
    }

    @Test
    void initializeWithStaticRegistry() {
        AlfredTelemetryHealthReporter reporter = new AlfredTelemetryHealthReporter();
        assertThat(reporter, is(notNullValue()));
    }

    @Test
    void activeCounter() {
        assertActiveGaugeEquals(0d);
        reporter.onStart();
        assertActiveGaugeEquals(1d);
        reporter.onCycleDone(null);
        assertActiveGaugeEquals(0d);
    }

    @Test
    void activePluginsCounter() {
        reporter.processReport(TestReports.healthy(), AssertHealthProcessorPlugin.class);
        assertThat(meterRegistry.get(Key.PLUGINS).gauge().value(), is(equalTo(1d)));
    }

    @Test
    void reportsCounter() {
        reporter.processReport(TestReports.healthy(), AssertHealthProcessorPlugin.class);
        reporter.processReport(TestReports.healthy(), NoOpHealthProcessorPlugin.class);

        assertThat(getReportGaugesValue(null, null), is(equalTo(2d)));
        assertThat(getReportGaugesValue("UNHEALTHY", null), is(equalTo(0d)));
        assertThat(getReportGaugesValue("HEALTHY", null), is(equalTo(2d)));
        assertThat(getReportGaugesValue("HEALTHY", "AssertHealthProcessorPlugin"), is(equalTo(1d)));
        assertThat(getReportGaugesValue(null, "NoOpHealthProcessorPlugin"), is(equalTo(1d)));

        reporter.processReport(TestReports.unhealthy(), AssertHealthProcessorPlugin.class);
        reporter.processReport(TestReports.unhealthy(), AssertHealthProcessorPlugin.class);

        assertThat(getReportGaugesValue(null, null), is(equalTo(4d)));
        assertThat(getReportGaugesValue("HEALTHY", null), is(equalTo(2d)));
        assertThat(getReportGaugesValue("UNHEALTHY", null), is(equalTo(2d)));
        assertThat(getReportGaugesValue("UNHEALTHY", "AssertHealthProcessorPlugin"), is(equalTo(2d)));
        assertThat(getReportGaugesValue(null, "NoOpHealthProcessorPlugin"), is(equalTo(1d)));
    }

    @Test
    void progress() {
        reporter.onProgress(new SimpleCycleProgress(1, 2, () -> 1));
        assertThat(meterRegistry.get(Key.PROGRESS).gauge().value(), is(closeTo(0.5, 0.0001)));

        reporter.onProgress(new SimpleCycleProgress(1, 2, () -> 2));
        assertThat(meterRegistry.get(Key.PROGRESS).gauge().value(), is(closeTo(1.0, 0.0001)));

        reporter.onProgress(new SimpleCycleProgress(1, 2, () -> 0));
        assertThat(meterRegistry.get(Key.PROGRESS).gauge().value(), is(closeTo(0.0, 0.0001)));

        reporter.onProgress(NullCycleProgress.getInstance());
        assertThat(meterRegistry.get(Key.PROGRESS).gauge().value(), is(Double.NaN));
    }

    private void assertActiveGaugeEquals(double expected) {
        assertThat(meterRegistry.get(Key.ACTIVE).gauge().value(), is(equalTo(expected)));
    }

    private double getReportGaugesValue(String status, String pluginClazz) {
        RequiredSearch search = meterRegistry.get(Key.REPORTS);
        if (StringUtils.hasText(status)) {
            search = search.tag(Tag.STATUS, status);
        }
        if (StringUtils.hasText(pluginClazz)) {
            search = search.tag(Tag.PLUGIN, pluginClazz);
        }

        return search.gauges().stream().mapToDouble(Gauge::value).sum();
    }

}
