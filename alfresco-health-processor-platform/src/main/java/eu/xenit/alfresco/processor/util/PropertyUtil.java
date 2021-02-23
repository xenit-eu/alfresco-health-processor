package eu.xenit.alfresco.processor.util;

import java.util.Properties;

public class PropertyUtil {

    private PropertyUtil() {
        // private ctor to hide implicit public one
    }

    public static Long getLong(Properties properties, String propertyKey) {
        return getOrDefaultLong(properties, propertyKey, null);
    }

    public static Long getOrDefaultLong(Properties properties, String propertyKey, Long defaultValue) {
        if (properties.containsKey(propertyKey)) {
            return Long.parseLong(properties.getProperty(propertyKey));
        }
        return defaultValue;
    }
}
