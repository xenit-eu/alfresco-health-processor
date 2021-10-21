package eu.xenit.alfresco.healthprocessor.extensibility.annotations;

import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Extension types used for {@link OnlyForUseIn} and {@link NotForUseIn} annotations.
 */
@InternalUseOnly
@AllArgsConstructor
public enum ExtensionType {
    /**
     * Extensions of type {@link HealthProcessorPlugin}
     */
    PROCESSOR(HealthProcessorPlugin.class),
    /**
     * Extensions of type {@link HealthReporter}
     */
    REPORTER(HealthReporter.class),
    /**
     * Extensions of type {@link HealthFixerPlugin}
     */
    FIXER(HealthFixerPlugin.class);

    @Getter
    private final Class<? extends BaseExtension> extensionInterface;
}
