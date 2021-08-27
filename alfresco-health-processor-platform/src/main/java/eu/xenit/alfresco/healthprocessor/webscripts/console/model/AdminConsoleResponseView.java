package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.module.ModuleDetails;

@Value
@AllArgsConstructor
public class AdminConsoleResponseView {

    String version;
    String status;

    IndexingStrategyView indexing;
    ExtensionsView plugins;
    ExtensionsView reporters;
    ExtensionsView fixers;

    public AdminConsoleResponseView(ModuleDetails moduleDetails, String status, IndexingStrategyView indexing,
            ExtensionsView plugins, ExtensionsView reporters, ExtensionsView fixers) {
        this(extractVersionNumber(moduleDetails), status, indexing, plugins, reporters, fixers);
    }

    private static String extractVersionNumber(ModuleDetails moduleDetails) {
        return moduleDetails == null ? "UNKNOWN" : moduleDetails.getModuleVersionNumber().toString();
    }
}
