package eu.xenit.alfresco.healthprocessor.plugins;

import eu.xenit.alfresco.healthprocessor.plugins.api.FilteredSingleNodeHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ThumbnailGenerationHealthProcessorPlugin extends FilteredSingleNodeHealthProcessorPlugin {

    private final ThumbnailService thumbnailService;

    public ThumbnailGenerationHealthProcessorPlugin(ServiceRegistry serviceRegistry) {
        this(serviceRegistry.getNodeService(), serviceRegistry.getDictionaryService(),
                serviceRegistry.getThumbnailService());
    }

    public ThumbnailGenerationHealthProcessorPlugin(NodeService nodeService, DictionaryService dictionaryService,
            ThumbnailService thumbnailService) {
        super(nodeService, dictionaryService);
        this.thumbnailService = thumbnailService;
    }

    @Override
    protected NodeHealthReport doProcess(NodeRef nodeRef) {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);

        ContentData contentData = (ContentData) props.get(ContentModel.PROP_CONTENT);
        if (contentData == null) {
            getLogger().debug("Skipping node '{}'. Node has no content.", nodeRef);
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
        }

        NodeHealthStatus status = NodeHealthStatus.HEALTHY;
        final List<String> messages = new ArrayList<>();

        List<ThumbnailDefinition> thumbnailDefinitions = thumbnailService.getThumbnailRegistry()
                .getThumbnailDefinitions(contentData.getMimetype(), contentData.getSize());

        for (ThumbnailDefinition thumbnailDefinition : thumbnailDefinitions) {
            final NodeRef existingThumbnail = this.thumbnailService.getThumbnailByName(nodeRef,
                    ContentModel.PROP_CONTENT, thumbnailDefinition.getName());

            if (existingThumbnail != null) {
                final String message = "Thumbnail '" + thumbnailDefinition.getName() + "' already exists: '"
                        + existingThumbnail + "'";
                messages.add(message);
                continue;
            }

            try {
                final NodeRef createdThumbnail =
                        thumbnailService.createThumbnail(
                                nodeRef,
                                ContentModel.PROP_CONTENT,
                                thumbnailDefinition.getMimetype(),
                                thumbnailDefinition.getTransformationOptions(),
                                thumbnailDefinition.getName());
                final String message = "Created thumbnail '" + thumbnailDefinition.getName() + "': " + createdThumbnail;
                getLogger().debug("Node '{}'. {}", nodeRef, message);
                messages.add(message);
            } catch (Exception e) {
                status = NodeHealthStatus.UNHEALTHY;
                final String message = "Failed to generate thumbnail '" + thumbnailDefinition.getName() + "': "
                        + e.getMessage();
                getLogger().info("Node '{}'. {}", nodeRef, message);
                getLogger().debug("Thumbnail generation failure", e);
                messages.add(message);
            }
        }

        return new NodeHealthReport(status, nodeRef, messages);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
