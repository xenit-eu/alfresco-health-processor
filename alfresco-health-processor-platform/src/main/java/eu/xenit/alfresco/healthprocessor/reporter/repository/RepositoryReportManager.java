package eu.xenit.alfresco.healthprocessor.reporter.repository;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.util.StreamUtils;

@RequiredArgsConstructor
@Slf4j
public class RepositoryReportManager {

    private final NodeService nodeService;
    private final FileFolderService fileFolderService;
    private final PermissionService permissionService;
    private final DirectoryService directoryService;
    private final NamespaceService namespaceService;

    private static final QName LATEST_REPORT = QName.createQName(HealthProcessorModel.HP_NAMESPACE, "latest");
    private static final QName CURRENT_REPORT = QName.createQName(HealthProcessorModel.HP_NAMESPACE, "current");
    private static final QName PROP_REMOVE_AFTER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
            "removeAfter");

    @Getter
    @Setter
    private int maxChunks;

    private QName[] reportsPath;

    @Getter
    @Setter
    private Duration keepReports;

    public void setReportsPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Reports path must start with /");
        }
        reportsPath = Arrays.stream(path.substring(1).split("/"))
                .map(pathPart -> QName.createQName(pathPart, namespaceService))
                .toArray(QName[]::new);
    }

    public String getReportsPath() {
        return "/" + Arrays.stream(reportsPath)
                .map(pathPart -> pathPart.toPrefixString(namespaceService))
                .collect(Collectors.joining("/"));
    }


    public Map<String, String> getConfiguration() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("max-chunks", Integer.toString(maxChunks));
        configuration.put("reports-path", Objects.toString(getReportsPath()));
        configuration.put("keep-reports", Objects.toString(keepReports));
        return configuration;
    }

    public NodeRef getOrCreateReportsRoot() {
        NodeRef storeRoot = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

        try {
            return directoryService.findChild(storeRoot, reportsPath);
        } catch (NoSuchNodeException e) {
            NodeRef newReportsFolder = directoryService.getOrCreateChild(storeRoot, reportsPath,
                    ContentModel.TYPE_FOLDER);
            nodeService.setProperty(newReportsFolder, ContentModel.PROP_NAME, "HealthProcessor reports");

            // Disable inheriting permissions when creating the folder for the first time
            permissionService.setInheritParentPermissions(newReportsFolder, false);
            return newReportsFolder;
        }
    }

    public FileInfo findReportLink(QName pathName) {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        return fileFolderService.getFileInfo(
                directoryService.findChild(reportsRoot, ContentModel.ASSOC_CONTAINS, pathName));
    }

    public FileInfo getOrCreateReportLink(String name, QName pathName) {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        try {
            return findReportLink(pathName);
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
        resetReportExpiration(newReportFolder);

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
        float storedProgress = DefaultTypeConverter.INSTANCE.floatValue(
                nodeService.getProperty(reportFolder, HealthProcessorModel.PROP_COMPLETION_PERCENTAGE));
        // Only update progress if a significant progress is made, to avoid continuously writing updates
        if (progress > storedProgress + 0.0001) {
            nodeService.setProperty(reportFolder, HealthProcessorModel.PROP_COMPLETION_PERCENTAGE, progress);
        }
        // If the report is almost expired, reset expiration timer
        resetReportExpirationIfAlmostExpired(reportFolder);
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

        // Reset expiration time, as report is now completed
        resetReportExpiration(reportFolder);

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
        FileInfo currentReportLink = findReportLink(CURRENT_REPORT);
        if (reportFolder.equals(currentReportLink.getLinkNodeRef())) {
            nodeService.setProperty(currentReportLink.getNodeRef(), ContentModel.PROP_LINK_DESTINATION, null);
        }
    }

    public void removeExpiredReports() {
        NodeRef reportsRoot = getOrCreateReportsRoot();
        List<ChildAssociationRef> childAssociationRefs = nodeService.getChildAssocs(reportsRoot,
                ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
        for (ChildAssociationRef childAssociationRef : childAssociationRefs) {
            Date removeAfter = DefaultTypeConverter.INSTANCE.convert(Date.class,
                    nodeService.getProperty(childAssociationRef.getChildRef(), PROP_REMOVE_AFTER));
            if (removeAfter != null && (new Date()).after(removeAfter)) {
                log.debug("Removing expired report {}", childAssociationRef.getChildRef());
                nodeService.removeChildAssociation(childAssociationRef);
            }
        }
    }

    private void resetReportExpirationIfAlmostExpired(NodeRef reportFolder) {
        Date expirationDate = DefaultTypeConverter.INSTANCE.convert(Date.class,
                nodeService.getProperty(reportFolder, PROP_REMOVE_AFTER));
        Duration timeLeftTillExpire = Duration.between(Instant.now(), expirationDate.toInstant());
        // Has already expired or is over half of the total expiration time, reset expiration
        if (timeLeftTillExpire.isNegative() || timeLeftTillExpire.compareTo(keepReports.dividedBy(2)) < 0) {
            resetReportExpiration(reportFolder);
        }
    }

    private void resetReportExpiration(NodeRef reportFolder) {
        nodeService.setProperty(reportFolder, PROP_REMOVE_AFTER, Date.from(Instant.now().plus(keepReports)));
    }
}
