package eu.xenit.alfresco.processor.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class HealthProcessorConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HealthProcessorConfiguration.class);

    private static String PROP_PROC_ENABLED = "eu.xenit.alfresco.processor.enabled";
    private static String PROP_PROC_TRACKING_ENABLED = "eu.xenit.alfresco.processor.tracking.enabled";
    private static String PROP_PROC_RUN_ONCE = "eu.xenit.alfresco.processor.run-once";
    private static String PROP_PROC_SCOPE = "eu.xenit.alfresco.processor.scope";
    private static String PROP_PROC_TXN_START = "eu.xenit.alfresco.processor.transaction.start";
    private static String PROP_PROC_TXN_LIMIT = "eu.xenit.alfresco.processor.transaction.limit";
    private static String PROP_PROC_TXN_COMMIT_TIME_INCREMENT = "eu.xenit.alfresco.processor.transaction.time.increment";
    private static String PROP_PROC_TXN_COMMIT_TIME_START = "eu.xenit.alfresco.processor.transaction.time.start";

    protected Properties globalProperties;

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

    public ProcessorService.TransactionScope getScope() {
            return getProperty(
                    PROP_PROC_SCOPE,
                    ProcessorService.TransactionScope.ALL,
                    this::createScope);
    }

    public int getTransactionLimit() {
        return getProperty(
                PROP_PROC_TXN_LIMIT,
                1000,
                Integer::parseInt);
    }

    public long getFirstTransaction() {
        return getProperty(
                PROP_PROC_TXN_START,
                1L,
                Long::parseLong);
    }

    public int getTimeIncrementSeconds() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_INCREMENT,
                15,
                Integer::parseInt);
    }

    public long getFirstCommitTime() {
        return LocalDate.parse(
                getFirstCommitTimeValue(),
                DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    private String getFirstCommitTimeValue() {
        return getProperty(
                PROP_PROC_TXN_COMMIT_TIME_START,
                LocalDate.now().minusYears(10)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE),
                String::toString);
    }

    <T> T getProperty(String propertyName, T defaultValue, PropertyConverter<T> convertor) {
        if(propertyName == null || propertyName.isEmpty()) {
            throw new IllegalStateException("Property name must be provided in order to resolve a value.");
        }

        if(!globalProperties.containsKey(propertyName)) {
            logger.debug("Property {} could not be found in the global configuration", propertyName);
            return defaultValue;
        }

        String prop = globalProperties.get(propertyName).toString();
        logger.debug("Global property value: {}", prop);
        return convertor.from(prop);
    }

    ProcessorService.TransactionScope createScope(String value) {
        if(value == null || value.isEmpty()) {
            throw new InvalidParameterException("value cannot be null or empty!");
        }
        return ProcessorService.TransactionScope.valueOf(value.toUpperCase());
    }

    interface PropertyConverter<T> {
        T from(String value);
    }
}
