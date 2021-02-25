package eu.xenit.alfresco.healthprocessor.plugins.api;

import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef;

public interface HealthProcessorPlugin {

    boolean isEnabled();

    void process(Set<NodeRef> nodeRefs);

}
