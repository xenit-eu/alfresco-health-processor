package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef;

public interface HealthProcessorPlugin {

    boolean isEnabled();

    Set<NodeHealthReport> process(Set<NodeRef> nodeRefs);

}
