package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
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

    default void onProgress(@Nonnull Class<? extends IndexingStrategy> indexingStrategyClass, @Nonnull
            IndexingProgress progress) {

    }

    default void onException(@Nonnull Exception e) {

    }

    HealthReporter disabled = new DisabledHealthReporter();
}
