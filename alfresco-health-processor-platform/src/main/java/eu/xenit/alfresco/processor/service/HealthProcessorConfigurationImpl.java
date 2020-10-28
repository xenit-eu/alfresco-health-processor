package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.PropertyConverter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class HealthProcessorConfigurationImpl implements HealthProcessorConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HealthProcessorConfigurationImpl.class);

    private static String PROP_PROC_ENABLED = "eu.xenit.alfresco.processor.enabled";
    private static String PROP_PROC_TRACKING_ENABLED = "eu.xenit.alfresco.processor.tracking.enabled";
    private static String PROP_PROC_RUN_ONCE = "eu.xenit.alfresco.processor.run-once";
    private static String PROP_PROC_SCOPE = "eu.xenit.alfresco.processor.scope";
    private static String PROP_PROC_TXN_START = "eu.xenit.alfresco.processor.transaction.start";
    private static String PROP_PROC_TXN_LIMIT = "eu.xenit.alfresco.processor.transaction.limit";
    private static String PROP_PROC_TXN_COMMIT_TIME_INCREMENT = "eu.xenit.alfresco.processor.transaction.time.increment";
    private static String PROP_PROC_TXN_COMMIT_TIME_START = "eu.xenit.alfresco.processor.transaction.time.start";

    @Getter @Setter
    protected Properties globalProperties;

    protected HealthProcessorConfigurationImpl() {}
    public HealthProcessorConfigurationImpl(Properties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Override
    public boolean isEnabled() {
        return getProperty(
                PROP_PROC_ENABLED,
                false,
                PropertyConverter.BOOLEAN);
    }

    @Override
    public boolean isRunOnce() {
        return getProperty(
                PROP_PROC_RUN_ONCE,
                false,
                PropertyConverter.BOOLEAN);
    }

    @Override
    public ProcessorService.Scope getScope() {
            return getProperty(
                    PROP_PROC_SCOPE,
                    ProcessorService.Scope.ALL,
                    PropertyConverter.SCOPE);
    }

    @Override
    public int getTransactionLimit() {
        return getProperty(
                PROP_PROC_TXN_LIMIT,
                1000,
                PropertyConverter.INTEGER);
    }

    @Override
    public long getFirstTransaction() {
        return getProperty(
                PROP_PROC_TXN_START,
                1L,
                PropertyConverter.LONG);
    }

    @Override
    public int getTimeIncrementSeconds() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_INCREMENT,
                15,
                PropertyConverter.INTEGER);
    }

    @Override
    public long getFirstCommitTime() {
        return LocalDate.parse(
                getFirstCommitTimeValue(),
                DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    @Override
    public String getFirstCommitTimeValue() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_START,
                LocalDate.now().minusYears(10)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE),
                PropertyConverter.STRING);
    }

    <T> T getProperty(String propertyName, T defaultValue, PropertyConverter<T> convertor) {
        if(propertyName == null || propertyName.length() < 1) {
            throw new IllegalStateException("Property name must be provided in order to resolve a value.");
        }

        if(!globalProperties.containsKey(propertyName)) {
            logger.warn("Property {} could not be found in the global configuration", propertyName);
            return defaultValue;
        }

        String prop = globalProperties.get(propertyName).toString();
        logger.debug("Global property value: {}", prop);
        return convertor.from(prop);
    }
}
