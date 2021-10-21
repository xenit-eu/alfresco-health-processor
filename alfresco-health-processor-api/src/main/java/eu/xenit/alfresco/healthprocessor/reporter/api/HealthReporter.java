package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Extension point for plugging in logic into the Health-Processor to report results.
 *
 * The methods of an HealthReporter are called at specific points in the Health-Processor lifecycle and can be used to
 * manage the report.
 *
 * @see ToggleableHealthReporter implementation that can be enabled via a property
 * @see SingleReportHealthReporter implementation dividing the batch in individual calls
 */
public interface HealthReporter extends BaseExtension {

    /**
     * Called when a new Health-Processor cycle is started.
     */
    default void onStart() {

    }

    /**
     * Called after a batch of nodes have been processed by an {@link HealthProcessorPlugin}.
     *
     * Any {@link eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin}s have also done their fixes before this
     * method is called, the reports received here are the final state for this cycle.
     *
     * @param pluginClass The plugin that has processed the batch of nodes.
     * @param reports Reports that were created by the plugin.
     */
    default void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {

    }

    /**
     * Called when a Health-Processor cycle has completed.
     *
     * @param overviews Summary of the results from the completed Health-Processor cycle.
     */
    default void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {

    }

    /**
     * Called when the Health-Processor cycle has made progress.
     *
     * @param progress Progress indicator into the current cycle.
     * @implNote This method is called every time after a new batch of nodes is processed.
     * @since 0.5.0
     */
    default void onProgress(@Nonnull CycleProgress progress) {

    }

    /**
     * Called when a Health-Processor cycle has failed.
     *
     * When an exception occurs during a Health-Processor cycle, the cycle is aborted and this method is called to
     * report the abnormal termination.
     *
     * @param exception The exception that cause the Health-Processor cycle to fail.
     */
    default void onException(@Nonnull Exception exception) {

    }

    /**
     * A disabled {@link HealthReporter} that always reports as not enabled.
     */
    HealthReporter disabled = DisabledHealthReporter.getInstance();
}
