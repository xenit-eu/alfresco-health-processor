package eu.xenit.alfresco.healthprocessor.extensibility;

import eu.xenit.alfresco.healthprocessor.extensibility.annotations.InternalUseOnly;
import java.util.Collections;
import java.util.Map;

/**
 * Base for all Health-Processor extensions.
 *
 * Do not implement this interface directly, use one of the extension interfaces for implementing your specific
 * extension.
 *
 * @since 0.5.0
 */
@InternalUseOnly
public interface BaseExtension {

    /**
     * Whether this extension is enabled or not
     *
     * @return Whether this extension is enabled or not
     */
    boolean isEnabled();

    /**
     * Exposes the internal runtime state of this extension.
     *
     * The runtime state is only used for human consumption in the admin dashboard. As such, it is recommended to use
     * readable strings.
     *
     * Exposing internal state is optional, but it is recommended to expose all variable fields in the extension to give
     * a clear overview of the state of the extension.
     *
     * @return Map of stringified runtime state of this extension
     */
    default Map<String, String> getState() {
        return Collections.emptyMap();
    }

    /**
     * Exposes the configuration of this extension.
     *
     * The configuration is only used for human consumption in the admin dashboard. As such, it is recommended to use
     * readable strings that match the (abridged) configuration properties used for this extension.
     *
     * Exposing configuration is optional, but it is recommended to expose all configuration properties that are used by
     * the extension, also those that are left to their default values, as to give a clear overview of the configuration
     * of the extension.
     *
     * @return Map of stringified configuration of this extension.
     */
    default Map<String, String> getConfiguration() {
        return Collections.singletonMap("enabled", Boolean.toString(isEnabled()));
    }

}
