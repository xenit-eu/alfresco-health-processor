package eu.xenit.alfresco.healthprocessor.reporter.api;

import lombok.Data;

@Data
public abstract class ToggleableHealthReporter implements HealthReporter {

    private boolean enabled;

}
