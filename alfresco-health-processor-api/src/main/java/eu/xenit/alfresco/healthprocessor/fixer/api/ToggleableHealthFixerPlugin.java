package eu.xenit.alfresco.healthprocessor.fixer.api;

import lombok.Getter;
import lombok.Setter;

public abstract class ToggleableHealthFixerPlugin implements HealthFixerPlugin {

    @Getter
    @Setter
    private boolean enabled;

}
