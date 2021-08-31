package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class ProcessorPluginOverview {

    @Nonnull
    Class<? extends HealthProcessorPlugin> pluginClass;
    @Nonnull
    Map<NodeHealthStatus, Long> countsByStatus;
    @Nonnull
    List<NodeHealthReport> reports;

}
