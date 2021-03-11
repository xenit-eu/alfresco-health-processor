package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.ClassUtils;

@Slf4j
public class AlfredTelemetryHealthReporterFactoryBean extends AbstractFactoryBean<HealthReporter> {

    private static final String CLASS_MICROMETER_METRICS = "io.micrometer.core.instrument.Metrics";

    private final boolean isMicrometerPresent;

    @SuppressWarnings("unused")
    public AlfredTelemetryHealthReporterFactoryBean() {
        this(ClassUtils.isPresent(CLASS_MICROMETER_METRICS, HealthReporter.class.getClassLoader()));
    }

    public AlfredTelemetryHealthReporterFactoryBean(boolean isMicrometerPresent) {
        this.isMicrometerPresent = isMicrometerPresent;
    }

    @Setter
    private boolean enabled;

    @Override
    public Class<?> getObjectType() {
        return HealthReporter.class;
    }

    @Override
    protected HealthReporter createInstance() {
        if (isMicrometerPresent) {
            AlfredTelemetryHealthReporter ret = new AlfredTelemetryHealthReporter();
            ret.setEnabled(enabled);
            return ret;
        }

        if (enabled) {
            log.warn("{} enabled but Micrometer not found on the classpath. Is Alfred Telemetry installed?",
                    AlfredTelemetryHealthReporter.class.getSimpleName());
        }
        return HealthReporter.disabled();
    }
}
