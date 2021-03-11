package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import eu.xenit.alfresco.healthprocessor.plugins.NoOpHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.TestReports;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Key;
import eu.xenit.alfresco.healthprocessor.reporter.telemetry.Constants.Tag;
import io.micrometer.core.instrument.Counter;
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
        reporter.onStop();
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

        assertThat(getReportCountersValue(null, null), is(equalTo(2d)));
        assertThat(getReportCountersValue("HEALTHY", null), is(equalTo(2d)));
        assertThat(getReportCountersValue("HEALTHY", "AssertHealthProcessorPlugin"), is(equalTo(1d)));
        assertThat(getReportCountersValue(null, "NoOpHealthProcessorPlugin"), is(equalTo(1d)));

        reporter.processReport(TestReports.unhealthy(), AssertHealthProcessorPlugin.class);
        reporter.processReport(TestReports.unhealthy(), AssertHealthProcessorPlugin.class);

        assertThat(getReportCountersValue(null, null), is(equalTo(4d)));
        assertThat(getReportCountersValue("HEALTHY", null), is(equalTo(2d)));
        assertThat(getReportCountersValue("UNHEALTHY", null), is(equalTo(2d)));
        assertThat(getReportCountersValue("UNHEALTHY", "AssertHealthProcessorPlugin"), is(equalTo(2d)));
        assertThat(getReportCountersValue(null, "NoOpHealthProcessorPlugin"), is(equalTo(1d)));
    }

    private void assertActiveGaugeEquals(double expected) {
        assertThat(meterRegistry.get(Key.ACTIVE).gauge().value(), is(equalTo(expected)));
    }

    private double getReportCountersValue(String status, String pluginClazz) {
        RequiredSearch search = meterRegistry.get(Key.REPORTS);
        if (StringUtils.hasText(status)) {
            search = search.tag(Tag.STATUS, status);
        }
        if (StringUtils.hasText(pluginClazz)) {
            search = search.tag(Tag.PLUGIN, pluginClazz);
        }

        return search.counters().stream().mapToDouble(Counter::count).sum();
    }

}