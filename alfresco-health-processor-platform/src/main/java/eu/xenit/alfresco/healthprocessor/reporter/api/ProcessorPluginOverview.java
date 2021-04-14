package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.EnumMap;
import java.util.List;
import lombok.Value;

@Value
public class ProcessorPluginOverview {

    Class<? extends HealthProcessorPlugin> pluginClass;
    EnumMap<NodeHealthStatus, Long> countsByStatus;
    List<NodeHealthReport> reports;

}
