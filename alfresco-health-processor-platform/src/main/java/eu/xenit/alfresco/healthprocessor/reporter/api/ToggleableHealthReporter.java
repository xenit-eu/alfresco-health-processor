package eu.xenit.alfresco.healthprocessor.reporter.api;

import lombok.Getter;
import lombok.Setter;

public abstract class ToggleableHealthReporter implements HealthReporter {

    @Getter
    @Setter
    private boolean enabled;

}
