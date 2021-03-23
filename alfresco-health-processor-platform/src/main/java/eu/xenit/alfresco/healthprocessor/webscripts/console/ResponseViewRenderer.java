package eu.xenit.alfresco.healthprocessor.webscripts.console;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.processing.ProcessorService;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.AdminConsoleResponseView;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.IndexingStrategyView;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.PluginsView;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.ReportersView;
import java.util.List;
import lombok.Setter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;

@Setter
public class ResponseViewRenderer {

    public static final String MODULE_ID = "alfresco-health-processor-platform";

    private ModuleDetails moduleDetails;
    private ProcessorService processorService;
    private IndexingConfiguration indexingConfiguration;
    private IndexingStrategy indexingStrategy;
    private List<HealthProcessorPlugin> plugins;
    private List<HealthReporter> reporters;

    @SuppressWarnings("unused")
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.moduleDetails = serviceRegistry.getModuleService().getModule(MODULE_ID);
    }

    public AdminConsoleResponseView renderView() {
        return new AdminConsoleResponseView(
                moduleDetails,
                processorService.isActive() ? "ACTIVE" : "IDLE",
                new IndexingStrategyView(indexingConfiguration, indexingStrategy),
                new PluginsView(plugins),
                new ReportersView(reporters)
        );
    }

}
