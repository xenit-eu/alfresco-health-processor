package eu.xenit.alfresco.healthprocessor.checker;

import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.EXCEPTION;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.NOT_INDEX_RELEVANT;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.FOUND;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.FOUND_OUTDATED;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.FOUND_PATH_MISSING;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.FOUND_UNDELETED;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.NOT_FOUND;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.slf4j.Logger;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.ElasticResult;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ElasticIndexValidationHealthProcessorPlugin extends SubsystemDependantHealthProcessorPlugin
{

    @EqualsAndHashCode.Exclude
    private final NodeService nodeService;

    private final SearchEndpointSelector<SearchEndpoint> elasticServerSelector;

    @EqualsAndHashCode.Exclude
    private final ElasticRequestExecutor elasticRequestExecutor;

    protected static final String MSG_NO_SEARCH_ENDPOINTS = "Node is not expected in any search index.";

    public ElasticIndexValidationHealthProcessorPlugin(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, NodeService nodeService, SearchEndpointSelector<SearchEndpoint> elasticServerSelector,
            ElasticRequestExecutor elasticRequestExecutor)
    {
        super(searchApplicationContextFactory, subsystemName);
        this.nodeService = nodeService;
        this.elasticServerSelector = elasticServerSelector;
        this.elasticRequestExecutor = elasticRequestExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger()
    {
        return log;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs)
    {
        Set<NodeHealthReport> healthReports = new HashSet<>(nodeRefs.size());
        Map<NodeRef.Status, Set<NodeIndexHealthReport<SearchEndpoint>>> indexHealthReports = new HashMap<>(nodeRefs.size());

        Set<NodeRef.Status> nodeRefStatuses = nodeRefs.stream().map(nodeService::getNodeStatus).collect(Collectors.toSet());

        Map<SearchEndpoint, Set<NodeRef.Status>> endpointToNodeMap = new HashMap<>();
        for (NodeRef.Status nodeRefStatus : nodeRefStatuses)
        {
            Set<SearchEndpoint> searchEndpoints = elasticServerSelector.getSearchEndpointsForNode(nodeRefStatus);
            getLogger().trace("Found endpoints for node {}: {}", nodeRefStatus.getNodeRef(), searchEndpoints);
            for (SearchEndpoint searchEndpoint : searchEndpoints)
            {
                endpointToNodeMap.computeIfAbsent(searchEndpoint, k -> new HashSet<>()).add(nodeRefStatus);
            }
            if (searchEndpoints.isEmpty())
            {
                getLogger().debug("Node {} has no search endpoints", nodeRefStatus.getNodeRef());
                healthReports.add(new NodeHealthReport(NodeHealthStatus.NONE, nodeRefStatus.getNodeRef(), MSG_NO_SEARCH_ENDPOINTS));
            }
            else
            {
                indexHealthReports.put(nodeRefStatus, new HashSet<>());
            }
        }

        for (Map.Entry<SearchEndpoint, Set<NodeRef.Status>> entry : endpointToNodeMap.entrySet())
        {
            SearchEndpoint searchEndpoint = entry.getKey();
            Set<NodeRef.Status> expectedNodeRefStatuses = new HashSet<>(entry.getValue());
            Set<NodeRef.Status> processedStatuses = new HashSet<>();
            BiConsumer<NodeRef.Status, IndexHealthStatus> processor = (nrs, ihs) -> {
                indexHealthReports.get(nrs).add(new NodeIndexHealthReport<>(ihs, nrs, searchEndpoint));
                processedStatuses.add(nrs);
            };
            try
            {
                ElasticResult searchResult = elasticRequestExecutor.checkNodeIndexed(searchEndpoint, expectedNodeRefStatuses);

                getLogger().trace("Search endpoint {}: expected nodes {}, result {}", searchEndpoint, expectedNodeRefStatuses,
                        searchResult);

                searchResult.getFound().stream().forEach(nrs -> processor.accept(nrs, FOUND));
                searchResult.getMissing().stream().forEach(nrs -> processor.accept(nrs, NOT_FOUND));
                searchResult.getPathMissing().stream().forEach(nrs -> processor.accept(nrs, FOUND_PATH_MISSING));
                searchResult.getOutdated().stream().forEach(nrs -> processor.accept(nrs, FOUND_OUTDATED));
                searchResult.getSuperfluous().stream().forEach(nrs -> processor.accept(nrs, FOUND_UNDELETED));

                expectedNodeRefStatuses.stream().filter(Predicate.not(processedStatuses::contains))
                        .forEach(nrs -> processor.accept(nrs, NOT_INDEX_RELEVANT));
            }
            catch (IOException exception)
            {
                getLogger().error("Exception during healthcheck on search endpoint {}", searchEndpoint, exception);
                for (Status status : expectedNodeRefStatuses)
                {
                    indexHealthReports.get(status).add(new NodeIndexHealthReport<>(EXCEPTION, status, searchEndpoint));
                }
            }
        }

        indexHealthReports.entrySet().stream().map(entry -> {
            Optional<IndexHealthStatus> highestHealthStatus = entry.getValue().stream().map(NodeIndexHealthReport::getHealthStatus)
                    .min(Comparator.comparingInt(IndexHealthStatus::ordinal));
            Set<String> messages = entry.getValue().stream().map(NodeIndexHealthReport::getMessage).collect(Collectors.toSet());
            NodeHealthReport healthReport = new NodeHealthReport(highestHealthStatus.get().getNodeHealthStatus(),
                    entry.getKey().getNodeRef(), messages);
            healthReport.data(NodeIndexHealthReport.class).addAll(entry.getValue());
            return healthReport;
        }).filter(Objects::nonNull).forEach(healthReports::add);

        return healthReports;
    }

    @Override
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("enabled", Boolean.toString(isEnabled()));
        configuration.put("subsystemName", subsystemName);
        configuration.put("elasticServerSelector", elasticServerSelector.toString());
        return configuration;
    }
}
