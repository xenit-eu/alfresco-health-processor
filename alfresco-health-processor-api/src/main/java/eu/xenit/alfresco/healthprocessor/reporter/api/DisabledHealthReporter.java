package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Set;
import javax.annotation.Nonnull;

final class DisabledHealthReporter implements HealthReporter {

    private static class DisabledHealthReporterHolder {

        private static final HealthReporter INSTANCE = new DisabledHealthReporter();
    }

    public static HealthReporter getInstance() {
        return DisabledHealthReporterHolder.INSTANCE;
    }

    private DisabledHealthReporter() {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {
        throw new UnsupportedOperationException("The DisabledHealthReporter shouldn't process reports");
    }
}
