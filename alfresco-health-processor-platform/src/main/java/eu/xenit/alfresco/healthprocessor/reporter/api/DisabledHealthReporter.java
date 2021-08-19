package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Set;
import javax.annotation.Nonnull;

public final class DisabledHealthReporter implements HealthReporter {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass, @Nonnull Set<NodeHealthReport> reports) {
        throw new UnsupportedOperationException("The DisabledHealthReporter shouldn't process reports");
    }
}
