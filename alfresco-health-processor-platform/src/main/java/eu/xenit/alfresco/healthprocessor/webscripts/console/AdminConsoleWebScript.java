package eu.xenit.alfresco.healthprocessor.webscripts.console;

import com.google.common.annotations.VisibleForTesting;
import eu.xenit.alfresco.healthprocessor.webscripts.console.AdminConsoleWebscriptResponseModel.HealthProcessorModule;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

@RequiredArgsConstructor
public class AdminConsoleWebScript extends DeclarativeWebScript {

    public final static String MODULE_ID = "alfresco-health-processor-platform";

    private final ModuleDetails moduleDetails;

    public AdminConsoleWebScript(ServiceRegistry serviceRegistry) {
        this(serviceRegistry.getModuleService().getModule(MODULE_ID));
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> model = new HashMap<>();

        model.put("healthprocessor", this.createResponseModel());

        return model;
    }

    @VisibleForTesting
    AdminConsoleWebscriptResponseModel createResponseModel() {
        HealthProcessorModule module = new HealthProcessorModule(
                moduleDetails.getId(),
                moduleDetails.getModuleVersionNumber().toString());

        return new AdminConsoleWebscriptResponseModel(module);
    }

}
