package eu.xenit.alfresco.healthprocessor.extensibility;

import java.util.Collections;
import java.util.Map;

public interface BaseExtension {
    boolean isEnabled();

    default Map<String, String> getState() {
        return Collections.emptyMap();
    }

    default Map<String, String> getConfiguration() {
        return Collections.singletonMap("enabled", Boolean.toString(isEnabled()));
    }

}
