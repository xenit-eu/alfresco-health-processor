package eu.xenit.alfresco.healthprocessor.metrics;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.ClassUtils;

public class MetricFactoryFactoryBean extends AbstractFactoryBean<MetricFactory> {

    private static final String CLASS_MICROMETER_METRICS = "io.micrometer.core.instrument.Metrics";

    private final boolean isMicrometerPresent;

    @SuppressWarnings("unused")
    public MetricFactoryFactoryBean() {
        this(ClassUtils.isPresent(CLASS_MICROMETER_METRICS, MetricFactory.class.getClassLoader()));
    }

    public MetricFactoryFactoryBean(boolean isMicrometerPresent) {
        this.isMicrometerPresent = isMicrometerPresent;
    }

    @Override
    public Class<?> getObjectType() {
        return MetricFactory.class;
    }

    @Override
    protected MetricFactory createInstance() {
        if (isMicrometerPresent) {
            return new MicrometerMetricFactory();
        } else {
            return new NullMetricFactory();
        }
    }
}
