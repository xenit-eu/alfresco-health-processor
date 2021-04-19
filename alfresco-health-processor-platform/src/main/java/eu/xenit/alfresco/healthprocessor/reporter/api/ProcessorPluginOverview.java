package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class ProcessorPluginOverview {

    Class<? extends HealthProcessorPlugin> pluginClass;
    Map<NodeHealthStatus, Long> countsByStatus;
    List<NodeHealthReport> reports;

}
