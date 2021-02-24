package eu.xenit.alfresco.processor.util;

import java.util.Properties;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyUtil {

    public static Boolean getBool(Properties properties, String propertyKey) {
        return getOrDefaultBool(properties, propertyKey, null);
    }

    public static Boolean getOrDefaultBool(Properties properties, String propertyKey, Boolean defaultValue) {
        return getProperty(properties, propertyKey, defaultValue, Boolean::parseBoolean);
    }

    public static Integer getInt(Properties properties, String propertyKey) {
        return getOrDefaultInt(properties, propertyKey, null);
    }

    public static Integer getOrDefaultInt(Properties properties, String propertyKey, Integer defaultValue) {
        return getProperty(properties, propertyKey, defaultValue, Integer::parseInt);
    }

    public static Long getLong(Properties properties, String propertyKey) {
        return getOrDefaultLong(properties, propertyKey, null);
    }

    public static Long getOrDefaultLong(Properties properties, String propertyKey, Long defaultValue) {
        return getProperty(properties, propertyKey, defaultValue, Long::parseLong);
    }

    public static <T> T getProperty(Properties properties, String propertyKey, T defaultValue,
            Function<String, T> convertor) {
        if (propertyKey == null || propertyKey.isEmpty()) {
            throw new IllegalStateException("Property name must be provided in order to resolve a value.");
        }

        if (!properties.containsKey(propertyKey)) {
            log.debug("Property '{}' not found, defaulting to: '{}'", propertyKey, defaultValue);
            return defaultValue;
        }

        String prop = properties.get(propertyKey).toString();
        log.debug("Property '{}' found, value: '{}'", propertyKey, defaultValue);
        return convertor.apply(prop);
    }
}
