package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.extensibility.annotations.ExtensionType;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.OnlyForUseIn;

/**
 * Status of {@link NodeHealthReport}
 */
public enum NodeHealthStatus {
    /**
     * Node is healthy according to the plugin.
     */
    HEALTHY,
    /**
     * Node is unhealthy according to the plugin.
     *
     * This means that the plugin has detected that there is something wrong with the node.
     */
    UNHEALTHY,
    /**
     * The plugin can not determine health of a node.
     *
     * This means that the plugin has no verdict on the health of the node because it does not support checking its
     * health.
     */
    NONE,
    /**
     * The plugin did not send any health report for a node.
     *
     * This status only for use in HealthReporters, {@link eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin}
     * or {@link eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin} should never return or act upon this
     * value.
     *
     * @since 0.5.0
     */
    @OnlyForUseIn(ExtensionType.REPORTER)
    UNREPORTED,
    /**
     * Node was unhealthy according to the plugin, but it is fixed by a {@link eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin}.
     *
     * This status only for use in HealthReporters, {@link eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin}
     * should never return this status. This status is automatically applied to a HealthReport when {@link
     * eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin} has fixed unhealthy nodes.
     *
     * @since 0.5.0
     */
    @OnlyForUseIn(ExtensionType.REPORTER)
    FIXED
}
