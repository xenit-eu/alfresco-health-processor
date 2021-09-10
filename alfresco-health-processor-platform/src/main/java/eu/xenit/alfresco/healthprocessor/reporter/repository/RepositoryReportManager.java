package eu.xenit.alfresco.healthprocessor.reporter.repository;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.util.StreamUtils;

@RequiredArgsConstructor
@Slf4j
public class RepositoryReportManager {

    private final DescriptorService descriptorService;
    private final NodeService nodeService;
    private final FileFolderService fileFolderService;
    private final PermissionService permissionService;
    private static final QName HEALTH_PROCESSOR_FOLDER = QName.createQName(HealthProcessorModel.HP_NAMESPACE,
            "HealthProcessor");
    private static final QName REPORTS_FOLDER = QName.createQName(HealthProcessorModel.HP_NAMESPACE, "reports");
    private static final QName LATEST_REPORT = QName.createQName(HealthProcessorModel.HP_NAMESPACE, "latest");
    private static final QName CURRENT_REPORT = QName.createQName(HealthProcessorModel.HP_NAMESPACE, "current");

    @Getter
    @Setter
    private int maxChunks;


    public Map<String, String> getConfiguration() {
        return Collections.singletonMap("max-chunks", Integer.toString(maxChunks));
    }

    public NodeRef getOrCreateHealthProcessorRoot() {
        NodeRef storeRoot = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        NodeRef systemSpace = findChild(storeRoot, ContentModel.ASSOC_CHILDREN,
                QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "system"));
        return getOrCreateChild(systemSpace, ContentModel.ASSOC_CHILDREN, HEALTH_PROCESSOR_FOLDER,
                ContentModel.TYPE_CONTAINER);
    }

    public NodeRef getOrCreateReportsRoot() {
        NodeRef healthProcessorRoot = getOrCreateHealthProcessorRoot();

        try {
            return findChild(healthProcessorRoot, ContentModel.ASSOC_CHILDREN, REPORTS_FOLDER);
        } catch (NoSuchNodeException e) {
            NodeRef newReportsFolder = getOrCreateChild(healthProcessorRoot, ContentModel.ASSOC_CHILDREN,
                    REPORTS_FOLDER,
                    ContentModel.TYPE_FOLDER);
            nodeService.setProperty(newReportsFolder, ContentModel.PROP_NAME, "HealthProcessor reports");
            // Disable inheriting permissions
            permissionService.setInheritParentPermissions(newReportsFolder, false);

            // Create folder that links to this reports folder, but only on first creation (so admin can delete the folder
            // from the company home
            NodeRef storeRoot = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            NodeRef companyHome = findChild(storeRoot, ContentModel.ASSOC_CHILDREN,
                    QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "company_home"));

            nodeService.addChild(companyHome, newReportsFolder, ContentModel.ASSOC_CONTAINS, REPORTS_FOLDER);

            return newReportsFolder;
        }
    }

    public FileInfo getReportLink(QName pathName) {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        return fileFolderService.getFileInfo(findChild(reportsRoot, ContentModel.ASSOC_CONTAINS, pathName));
    }

    public FileInfo getOrCreateReportLink(String name, QName pathName) {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        try {
            return getReportLink(pathName);
        } catch (NoSuchNodeException e) {
            log.info("Creating " + pathName + " link");
            return fileFolderService.create(reportsRoot, name, ContentModel.TYPE_LINK, pathName);
        }
    }

    public NodeRef createNewReport(String cycleId) {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        Instant creationTime = Instant.now();
        NodeRef newReportFolder = fileFolderService.create(reportsRoot, "report_" + creationTime.toEpochMilli(),
                HealthProcessorModel.TYPE_REPORTS).getNodeRef();
        nodeService.setProperty(newReportFolder, ContentModel.PROP_DESCRIPTION, "Report created at " + creationTime);
        nodeService.setProperty(newReportFolder, HealthProcessorModel.PROP_COMPLETION_PERCENTAGE, 0);
        nodeService.setProperty(newReportFolder, HealthProcessorModel.PROP_CYCLE_ID, cycleId);
        nodeService.setProperty(newReportFolder, HealthProcessorModel.PROP_REPOSITORY_ID,
                descriptorService.getCurrentRepositoryDescriptor().getId());

        FileInfo currentReportLink = getOrCreateReportLink("In-Progress Report", CURRENT_REPORT);

        // Link new report folder
        nodeService.setProperty(currentReportLink.getNodeRef(), ContentModel.PROP_LINK_DESTINATION, newReportFolder);

        return newReportFolder;
    }


    public ContentWriter createNewReportChunk(NodeRef reportFolder, Class<? extends HealthProcessorPlugin> plugin,
            AtomicInteger sequence) {
        Instant creationTime = Instant.now();
        int currentSequence = sequence.incrementAndGet();
        if (currentSequence % maxChunks == 0) {
            log.debug("We have #{} chunks for plugin {} now. Time to compact chunks into an aggregate.", sequence,
                    plugin.getName());
            NodeRef aggregateReport = createNewReportAggregate(reportFolder, plugin);
            nodeService.setProperty(aggregateReport, ContentModel.PROP_NAME,
                    "aggregate_chunk_" + plugin.getName() + "_" + creationTime.toEpochMilli());
            nodeService.setProperty(aggregateReport, HealthProcessorModel.PROP_CHUNK_SEQUENCE,
                    sequence.incrementAndGet());
            log.debug("Aggregated chunks for plugin {} to {}", plugin.getName(), aggregateReport);
        }
        FileInfo chunkNode = fileFolderService.create(reportFolder,
                "chunk_" + plugin.getName() + "_" + sequence + "_" + creationTime.toEpochMilli(),
                HealthProcessorModel.TYPE_REPORT);
        log.debug("Created chunk for plugin {} with sequence {}: {}", plugin.getName(), sequence,
                chunkNode.getNodeRef());
        nodeService.setProperty(chunkNode.getNodeRef(), ContentModel.PROP_DESCRIPTION,
                "Report chunk for " + plugin.getName() + " with sequence " + sequence);
        nodeService.setProperty(chunkNode.getNodeRef(), HealthProcessorModel.PROP_PLUGIN, plugin.getName());
        nodeService.setProperty(chunkNode.getNodeRef(), HealthProcessorModel.PROP_CHUNK_SEQUENCE, sequence);
        return fileFolderService.getWriter(chunkNode.getNodeRef());
    }

    private List<NodeRef> getReportChunks(NodeRef reportFolder, Class<? extends HealthProcessorPlugin> plugin) {
        List<ChildAssociationRef> childAssociationsForPlugin = nodeService.getChildAssocsByPropertyValue(reportFolder,
                HealthProcessorModel.PROP_PLUGIN, plugin.getName());
        return childAssociationsForPlugin.stream().map(ChildAssociationRef::getChildRef)
                .filter(nodeRef -> nodeService.hasAspect(nodeRef, HealthProcessorModel.ASPECT_CHUNK))
                .sorted(Comparator.comparingInt(nodeRef -> DefaultTypeConverter.INSTANCE.intValue(
                        nodeService.getProperty(nodeRef, HealthProcessorModel.PROP_CHUNK_SEQUENCE))))
                .collect(Collectors.toList());
    }

    public void updateProgress(NodeRef reportFolder, float progress) {
        nodeService.setProperty(reportFolder, HealthProcessorModel.PROP_COMPLETION_PERCENTAGE, progress);
    }

    public NodeRef createNewReportAggregate(NodeRef reportFolder, Class<? extends HealthProcessorPlugin> plugin) {
        FileInfo aggregateNode = fileFolderService.create(reportFolder, "plugin_" + plugin.getName(),
                HealthProcessorModel.TYPE_REPORT);
        nodeService.setProperty(aggregateNode.getNodeRef(), ContentModel.PROP_DESCRIPTION,
                "Report for " + plugin.getName());
        nodeService.setProperty(aggregateNode.getNodeRef(), HealthProcessorModel.PROP_PLUGIN, plugin.getName());

        List<NodeRef> reportChunks = getReportChunks(reportFolder, plugin);
        log.debug("Merging #{} chunks for plugin {}", reportChunks.size(), plugin.getName());
        log.trace("Chunks to merge: {}", reportChunks);

        long startTime = System.currentTimeMillis();

        try (OutputStream aggregateOutputStream = fileFolderService.getWriter(aggregateNode.getNodeRef())
                .getContentOutputStream()) {
            for (NodeRef chunk : reportChunks) {
                try (InputStream chunkInputStream = fileFolderService.getReader(chunk).getContentInputStream()) {
                    StreamUtils.copy(chunkInputStream, aggregateOutputStream);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create an aggregate report: ", exception);
        }
        long endTime = System.currentTimeMillis();
        log.debug("Finished writing aggregate report for plugin {} in {}", plugin.getName(),
                DurationFormatUtils.formatDurationHMS(endTime - startTime));

        // Remove all temporary chunks
        reportChunks.forEach(nodeService::deleteNode);
        log.trace("Cleaned up merged chunks");

        return aggregateNode.getNodeRef();
    }

    public void finishReport(NodeRef reportFolder) {
        FileInfo latestReportLink = getOrCreateReportLink("Latest Report", LATEST_REPORT);

        // Link new report folder
        nodeService.setProperty(latestReportLink.getNodeRef(), ContentModel.PROP_LINK_DESTINATION, reportFolder);

        // Remove incomplete report marker
        nodeService.removeAspect(reportFolder, HealthProcessorModel.ASPECT_INCOMPLETE_REPORT);

        // Calculate totals rollup for report folder
        log.debug("Calculating totals for report folder");
        List<ChildAssociationRef> finalReportChildren = nodeService.getChildAssocs(reportFolder,
                Collections.singleton(ContentModel.ASSOC_CONTAINS));
        Set<NodeRef> unhealthyNodes = new HashSet<>();
        Set<NodeRef> fixedNodes = new HashSet<>();
        finalReportChildren.forEach(childAssociationRef -> {
            unhealthyNodes.addAll(DefaultTypeConverter.INSTANCE.getCollection(NodeRef.class,
                    nodeService.getProperty(childAssociationRef.getChildRef(),
                            HealthProcessorModel.PROP_UNHEALTHY_NODES)));
            fixedNodes.addAll(DefaultTypeConverter.INSTANCE.getCollection(NodeRef.class,
                    nodeService.getProperty(childAssociationRef.getChildRef(), HealthProcessorModel.PROP_FIXED_NODES)));
        });
        nodeService.setProperty(reportFolder, HealthProcessorModel.PROP_UNHEALTHY_NODES,
                new ArrayList<>(unhealthyNodes));
        nodeService.setProperty(reportFolder, HealthProcessorModel.PROP_FIXED_NODES, new ArrayList<>(fixedNodes));
        log.debug("Finished calculating totals for report folder");

        // Remove current report link
        FileInfo currentReportLink = getReportLink(CURRENT_REPORT);
        if (reportFolder.equals(currentReportLink.getLinkNodeRef())) {
            nodeService.setProperty(currentReportLink.getNodeRef(), ContentModel.PROP_LINK_DESTINATION, null);
        }
    }

    private NodeRef findChild(NodeRef parent, QNamePattern assocType, QNamePattern assocName) {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(parent, assocType, assocName);
        switch (children.size()) {
            case 0:
                throw new NoSuchNodeException(parent, assocType, assocName);
            case 1:
                return children.get(0).getChildRef();
            default:
                throw new DuplicateNodeException(parent, assocType, assocName);
        }
    }

    private NodeRef getOrCreateChild(NodeRef parent, QName assocType, QName assocName, QName newType) {
        try {
            return findChild(parent, assocType, assocName);
        } catch (NoSuchNodeException e) {
            log.info("Creating " + assocName + " folder because it does not exist.", e);
            return nodeService.createNode(parent, assocType, assocName, newType).getChildRef();
        }
    }

}
