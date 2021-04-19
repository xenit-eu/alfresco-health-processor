package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Set;

public final class DisabledHealthReporter implements HealthReporter {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void processReports(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> reports) {
        throw new UnsupportedOperationException("The DisabledHealthReporter shouldn't process reports");
    }
}
