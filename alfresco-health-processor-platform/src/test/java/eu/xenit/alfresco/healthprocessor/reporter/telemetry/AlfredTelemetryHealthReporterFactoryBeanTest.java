package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.reporter.api.DisabledHealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import org.junit.jupiter.api.Test;

class AlfredTelemetryHealthReporterFactoryBeanTest {

    @Test
    void getObjectType() {
        AlfredTelemetryHealthReporterFactoryBean factoryBean = new AlfredTelemetryHealthReporterFactoryBean();
        assertThat(factoryBean.getObjectType(), is(equalTo(HealthReporter.class)));
    }

    @Test
    void createInstance_micrometerPresent_enabled() {
        assertCreateInstance(true, true, AlfredTelemetryHealthReporter.class, true);
    }

    @Test
    void createInstance_micrometerNotPresent_enabled() {
        assertCreateInstance(false, true, DisabledHealthReporter.class, false);
    }

    @Test
    void createInstance_micrometerPresent_notEnabled() {
        assertCreateInstance(true, false, AlfredTelemetryHealthReporter.class, false);
    }

    @Test
    void createInstance_micrometerNotPresent_notEnabled() {
        assertCreateInstance(false, false, DisabledHealthReporter.class, false);
    }

    private void assertCreateInstance(boolean isMicrometerPresent, boolean isEnabled,
            Class<? extends HealthReporter> expectedClass, boolean expectedIsEnabled) {
        AlfredTelemetryHealthReporterFactoryBean factoryBean = createFactoryBean(isMicrometerPresent, isEnabled);
        HealthReporter createdBean = factoryBean.createInstance();
        assertThat(createdBean, is(instanceOf(expectedClass)));
        assertThat(createdBean.isEnabled(), is(equalTo(expectedIsEnabled)));
    }

    private AlfredTelemetryHealthReporterFactoryBean createFactoryBean(boolean isMicrometerPresent, boolean isEnabled) {
        AlfredTelemetryHealthReporterFactoryBean ret =
                new AlfredTelemetryHealthReporterFactoryBean(isMicrometerPresent);
        ret.setEnabled(isEnabled);
        return ret;
    }

}