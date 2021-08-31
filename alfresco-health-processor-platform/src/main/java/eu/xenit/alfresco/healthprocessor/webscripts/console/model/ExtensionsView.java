package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Value;

public class ExtensionsView {
    @Getter
    private final List<Extension> extensions;

    public ExtensionsView(List<? extends BaseExtension> extensions) {
        this.extensions = toViewModel(extensions);
    }

    private static List<Extension> toViewModel(List<? extends BaseExtension> extensions) {
        if (extensions == null) {
            return Collections.emptyList();
        }
        return extensions.stream()
                .map(ExtensionsView::toViewModel)
                .collect(Collectors.toList());
    }

    private static Extension toViewModel(BaseExtension extension) {
        return new Extension(extension.getClass().getSimpleName(), extension.getConfiguration(), extension.getState());
    }

    @Value
    public static class Extension {

        String name;
        Map<String, String> configuration;
        Map<String, String> state;
    }

}
