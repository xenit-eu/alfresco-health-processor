package eu.xenit.alfresco.healthprocessor.webscripts.console;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.processing.ProcessorService;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.AdminConsoleResponseView;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.ExtensionsView;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.IndexingStrategyView;
import java.util.List;
import lombok.Setter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;

@Setter
public class ResponseViewRendererImpl implements ResponseViewRenderer {

    public static final String MODULE_ID = "alfresco-health-processor-platform";

    private ModuleDetails moduleDetails;
    private ProcessorService processorService;
    private IndexingConfiguration indexingConfiguration;
    private IndexingStrategy indexingStrategy;
    private List<HealthProcessorPlugin> plugins;
    private List<HealthReporter> reporters;
    private List<HealthFixerPlugin> fixers;

    @SuppressWarnings("unused")
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.moduleDetails = serviceRegistry.getModuleService().getModule(MODULE_ID);
    }

    @Override
    public AdminConsoleResponseView renderView() {
        return new AdminConsoleResponseView(
                moduleDetails,
                processorService.getState().toString(),
                new IndexingStrategyView(indexingConfiguration, indexingStrategy),
                new ExtensionsView(plugins),
                new ExtensionsView(reporters),
                new ExtensionsView(fixers)
        );
    }

}
