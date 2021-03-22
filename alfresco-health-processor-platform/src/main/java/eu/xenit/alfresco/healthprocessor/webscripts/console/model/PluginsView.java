package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Value;

@Getter
public class PluginsView {

    List<Plugin> plugins;

    public PluginsView(List<HealthProcessorPlugin> plugins) {
        this.plugins = toViewModel(plugins);
    }

    private static List<Plugin> toViewModel(List<HealthProcessorPlugin> plugins) {
        if (plugins == null) {
            return Collections.emptyList();
        }
        return plugins.stream()
                .map(PluginsView::toViewModel)
                .collect(Collectors.toList());
    }

    private static Plugin toViewModel(HealthProcessorPlugin plugin) {
        return new Plugin(plugin.getClass().getSimpleName(), plugin.isEnabled());
    }

    @Value
    public static class Plugin {

        String name;
        boolean enabled;
    }

}