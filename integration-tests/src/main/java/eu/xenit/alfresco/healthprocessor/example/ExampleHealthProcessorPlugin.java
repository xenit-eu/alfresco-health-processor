package eu.xenit.alfresco.healthprocessor.example;

import eu.xenit.alfresco.healthprocessor.plugins.api.SingleNodeHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.Objects;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleHealthProcessorPlugin extends SingleNodeHealthProcessorPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ExampleHealthProcessorPlugin.class);

    private final NodeService nodeService;

    /**
     * For testing purposes, we keep track of the number of nodes this plugin has processed.
     */
    private long numberOfNodesProcessed;

    public ExampleHealthProcessorPlugin(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected NodeHealthReport process(NodeRef nodeRef) {
        if (!StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(nodeRef.getStoreRef())) {
            logger.debug("Ignoring '{}', since we are only interested in workspace://SpacesStore nodes", nodeRef);
            return null;
        }
        numberOfNodesProcessed++;

        if (!nodeService.exists(nodeRef) || nodeService.getNodeStatus(nodeRef).isDeleted()) {
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef, "Node does not exist or is deleted");
        }
        if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_TEMPORARY)) {
            return new NodeHealthReport(NodeHealthStatus.UNHEALTHY, nodeRef);
        }
        NodeHealthStatus status = nodeService.hasAspect(nodeRef, ContentModel.ASPECT_TEMPORARY) ?
                NodeHealthStatus.UNHEALTHY : NodeHealthStatus.HEALTHY;

        return new NodeHealthReport(status, nodeRef);
    }

    public long getNumberOfNodesProcessed() {
        return numberOfNodesProcessed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ExampleHealthProcessorPlugin that = (ExampleHealthProcessorPlugin) o;
        return numberOfNodesProcessed == that.numberOfNodesProcessed && Objects
                .equals(nodeService, that.nodeService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeService, numberOfNodesProcessed);
    }
}
