package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        return new Extension(extension.getClass().getSimpleName(), getDataOrException(extension, BaseExtension::getConfiguration), getDataOrException(extension, BaseExtension::getState));
    }

    private static Map<String, String> getDataOrException(BaseExtension extension, Function<BaseExtension, Map<String, String>> getter) {
        try {
            return getter.apply(extension);
        } catch(Exception e) {
            log.error("Failed to get data from extension {}", extension, e);
            return Collections.singletonMap("?!", e.toString());
        }
    }

    @Value
    public static class Extension {

        String name;
        Map<String, String> configuration;
        Map<String, String> state;
    }

}
