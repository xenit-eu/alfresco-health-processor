package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public interface HealthReporter {

    boolean isEnabled();

    default void onStart() {

    }

    default void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass, @Nonnull Set<NodeHealthReport> reports) {

    }

    default void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {

    }

    default void onException(@Nonnull Exception e) {

    }

    HealthReporter disabled = new DisabledHealthReporter();
}
