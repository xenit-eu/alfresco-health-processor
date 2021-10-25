package eu.xenit.alfresco.healthprocessor.reporter.api;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link HealthReporter} that already has an <code>enabled</code> property
 */
public abstract class ToggleableHealthReporter implements HealthReporter {

    @Getter
    @Setter
    private boolean enabled;

}
