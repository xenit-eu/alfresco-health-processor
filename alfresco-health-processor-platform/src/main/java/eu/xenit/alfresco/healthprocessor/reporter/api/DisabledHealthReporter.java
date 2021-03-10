package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Set;

public final class DisabledHealthReporter implements HealthReporter {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void processReports(Set<NodeHealthReport> reports, Class<? extends HealthProcessorPlugin> pluginClass) {

    }
}
