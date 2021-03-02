package eu.xenit.alfresco.healthprocessor.plugins;

import eu.xenit.alfresco.healthprocessor.plugins.api.SingleNodeHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.QNameUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;

@Slf4j
public class ContentValidationHealthProcessorPlugin extends SingleNodeHealthProcessorPlugin {

    private final NodeService nodeService;
    private final ContentService contentService;

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
            return null;
        }

        boolean nodeHasContent = false;
        Set<QName> failures = new HashSet<>();
        for (QName dContentPropertyKey : propertyQNamesToValidate) {
            getLogger().debug("Validating d:content property '{}' for node '{}'", dContentPropertyKey, nodeRef);
            ContentReader reader = contentService.getReader(nodeRef, dContentPropertyKey);
            if (reader == null) {
                getLogger().debug("No ContentRead found for property '{}', node '{}', skipping.", dContentPropertyKey,
                        nodeRef);
                continue;
            }
            getLogger().debug("ContentReader found for property '{}', node '{}'. Going to check if exists",
                    dContentPropertyKey, nodeRef);
            nodeHasContent = true;
            if (!reader.exists()) {
                failures.add(dContentPropertyKey);
            }
        }

        NodeHealthStatus status = nodeHasContent ?
                failures.isEmpty() ? NodeHealthStatus.HEALTHY : NodeHealthStatus.UNHEALTHY
                : NodeHealthStatus.NONE;
        return new NodeHealthReport(status, nodeRef, toStringSet(failures));
    }

    private static Set<String> toStringSet(Set<QName> qNames) {
        return qNames.stream().map(QName::toString).collect(Collectors.toSet());
    }
}
