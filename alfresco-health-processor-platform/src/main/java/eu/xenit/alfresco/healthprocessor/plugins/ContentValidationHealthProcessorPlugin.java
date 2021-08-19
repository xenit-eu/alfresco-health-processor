package eu.xenit.alfresco.healthprocessor.plugins;

import eu.xenit.alfresco.healthprocessor.plugins.api.SingleNodeHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.QNameUtil;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ContentValidationHealthProcessorPlugin extends SingleNodeHealthProcessorPlugin {

    private final NodeService nodeService;
    private final ContentService contentService;

    @Getter(value = AccessLevel.PACKAGE)
    private final Collection<QName> propertyQNamesToValidate;

    public ContentValidationHealthProcessorPlugin(ServiceRegistry serviceRegistry,
            Collection<String> propertyQNamesToValidate) {
        this(
                serviceRegistry.getNodeService(),
                serviceRegistry.getContentService(),
                serviceRegistry.getDictionaryService(),
                QNameUtil.toQNames(propertyQNamesToValidate, serviceRegistry.getNamespaceService())
        );
    }

    public ContentValidationHealthProcessorPlugin(NodeService nodeService, ContentService contentService,
            DictionaryService dictionaryService, Collection<QName> propertyQNamesToValidate) {
        ParameterCheck.mandatory("nodeService", nodeService);
        ParameterCheck.mandatory("contentService", contentService);

        this.nodeService = nodeService;
        this.contentService = contentService;

        if (propertyQNamesToValidate == null || propertyQNamesToValidate.isEmpty()) {
            this.propertyQNamesToValidate = dictionaryService.getAllProperties(DataTypeDefinition.CONTENT);
        } else {
            this.propertyQNamesToValidate = propertyQNamesToValidate;
        }

        getLogger().info("Initialized. Properties: '{}'", this.propertyQNamesToValidate);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected NodeHealthReport process(NodeRef nodeRef) {
        if (!nodeService.exists(nodeRef) || nodeService.getNodeStatus(nodeRef).isDeleted()) {
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef, "Node does not exist or is deleted");
        }

        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

        boolean nodeHasContent = false;
        Map<QName, String> failedPropertiesWithContentUrl = new HashMap<>();
        for (QName dContentPropertyKey : propertyQNamesToValidate) {
            String contentUrl = safeExtractContentUrl(properties, nodeRef, dContentPropertyKey);
            if (contentUrl == null) {
                continue;
            }

            getLogger().debug("Node '{}', property '{}', will try to retrieve ContentReader (ContentUrl: '{}')",
                    nodeRef, dContentPropertyKey, contentUrl);
            ContentReader reader = contentService.getRawReader(contentUrl);
            nodeHasContent = true;
            if (reader == null || !reader.exists()) {
                failedPropertiesWithContentUrl.put(dContentPropertyKey, contentUrl);
            }
        }

        NodeHealthStatus status;
        if (nodeHasContent) {
            status = failedPropertiesWithContentUrl.isEmpty() ? NodeHealthStatus.HEALTHY : NodeHealthStatus.UNHEALTHY;
        } else {
            status = NodeHealthStatus.NONE;
        }

        return new NodeHealthReport(status, nodeRef, toMessages(failedPropertiesWithContentUrl));
    }

    private String safeExtractContentUrl(Map<QName, Serializable> properties, NodeRef nodeRef,
            QName dContentPropertyKey) {
        if (!properties.containsKey(dContentPropertyKey)) {
            getLogger().trace("Node '{}', properties don't contain d:content property '{}', skipping.",
                    nodeRef, dContentPropertyKey);
            return null;
        }

        Serializable contentDataValue = properties.get(dContentPropertyKey);
        if (contentDataValue == null) {
            getLogger().debug("Node '{}', property '{}', ContentData is null", nodeRef, dContentPropertyKey);
            return null;
        }
        if (!(contentDataValue instanceof ContentData)) {
            getLogger().warn("Node '{}', property '{}', not of type d:content",
                    nodeRef, dContentPropertyKey);
            return null;
        }
        ContentData contentData = (ContentData) contentDataValue;
        if (contentData.getContentUrl() == null) {
            getLogger().debug("Node '{}', property '{}', ContentUrl is null",
                    nodeRef, dContentPropertyKey);
            return null;
        }
        return contentData.getContentUrl();
    }

    private static Set<String> toMessages(Map<QName, String> propQNamesWithContentUrl) {
        return propQNamesWithContentUrl.entrySet().stream()
                .map(entry -> toMessage(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    private static String toMessage(QName property, String contentUrl) {
        return "Property: '" + property + "', contentUrl: '" + contentUrl + "'";
    }
}
