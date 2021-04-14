package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.List;
import java.util.Set;

public interface HealthReporter {

    boolean isEnabled();

    default void onStart() {

    }

    default void processReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {

    }

    default void onCycleDone(List<ProcessorPluginOverview> overviews) {

    }

    default void onException(Exception e) {

    }

    HealthReporter disabled = new DisabledHealthReporter();
}
