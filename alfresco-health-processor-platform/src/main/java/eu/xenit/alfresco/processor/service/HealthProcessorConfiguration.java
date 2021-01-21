package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Function;

@AllArgsConstructor
public class HealthProcessorConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HealthProcessorConfiguration.class);

    private static String PROP_PROC_ENABLED = "eu.xenit.alfresco.processor.enabled";
    private static String PROP_PROC_TRACKING_ENABLED = "eu.xenit.alfresco.processor.tracking.enabled";
    private static String PROP_PROC_RUN_ONCE = "eu.xenit.alfresco.processor.run-once";
    private static String PROP_PROC_TXN_START = "eu.xenit.alfresco.processor.transaction.start";
    private static String PROP_PROC_TXN_LIMIT = "eu.xenit.alfresco.processor.transaction.batch-size";
    private static String PROP_PROC_TXN_COMMIT_TIME_INCREMENT = "eu.xenit.alfresco.processor.transaction.time.increment";
    private static String PROP_PROC_TXN_COMMIT_TIME_START = "eu.xenit.alfresco.processor.transaction.time.start";

    protected final Properties globalProperties;

    public boolean isEnabled() {
        return getProperty(
                PROP_PROC_ENABLED,
                false,
                Boolean::parseBoolean);
    }

    public boolean isRunOnce() {
        return getProperty(
                PROP_PROC_RUN_ONCE,
                false,
                Boolean::parseBoolean);
    }

    public long getTransactionLimit() {
        return getProperty(
                PROP_PROC_TXN_LIMIT,
                1000L,
                Long::parseLong);
    }

    public long getFirstTransaction() {
        return getProperty(
                PROP_PROC_TXN_START,
                1L,
                Long::parseLong);
    }

    public long getTimeIncrementSeconds() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_INCREMENT,
                15L,
                Long::parseLong);
    }

    public long getFirstCommitTime() {
        return DateTimeUtil.dateToEpochMs(getFirstCommitTimeValue());
    }

    private String getFirstCommitTimeValue() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_START,
                DateTimeUtil.fromTheLastXYears(10),
                String::toString);
    }

    <T> T getProperty(String propertyName, T defaultValue, Function<String, T> convertor) {
        if(propertyName == null || propertyName.isEmpty()) {
            throw new IllegalStateException("Property name must be provided in order to resolve a value.");
        }

        if(!globalProperties.containsKey(propertyName)) {
            logger.debug("Property {} could not be found in the global configuration", propertyName);
            return defaultValue;
        }

        String prop = globalProperties.get(propertyName).toString();
        logger.debug("Global property value: {}", prop);
        return convertor.apply(prop);
    }
}
