package eu.xenit.alfresco.healthprocessor.fixer.api;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link HealthFixerPlugin} that already has an <code>enabled</code> property
 *
 * @since 0.5.0
 */
public abstract class ToggleableHealthFixerPlugin implements HealthFixerPlugin {

    @Getter
    @Setter
    private boolean enabled;

}
