package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Set;

public interface HealthReporter {

    boolean isEnabled();

    default void onStart() {

    }

    void processReports(Set<NodeHealthReport> reports, Class<? extends HealthProcessorPlugin> pluginClass);

    default void onStop() {

    }

    static HealthReporter DISABLED() {
        return new DisabledHealthReporter();
    }

}
