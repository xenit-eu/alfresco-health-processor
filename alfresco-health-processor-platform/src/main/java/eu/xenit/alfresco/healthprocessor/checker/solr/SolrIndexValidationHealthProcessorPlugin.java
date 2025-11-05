package eu.xenit.alfresco.healthprocessor.checker.solr;

import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.DUPLICATE;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.EXCEPTION;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.FOUND;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.NOT_FOUND;
import static eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus.NOT_INDEXED;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.slf4j.Logger;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.checker.SubsystemDependantHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.SolrSearchResult;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class SolrIndexValidationHealthProcessorPlugin extends SubsystemDependantHealthProcessorPlugin
{

    protected static final String MSG_NO_SEARCH_ENDPOINTS = "Node is not expected in any search index.";

    @EqualsAndHashCode.Exclude
    private final NodeService nodeService;

    private final SearchEndpointSelector<SolrEndpoint> solrServerSelector;

    @EqualsAndHashCode.Exclude
    private final SolrRequestExecutor solrRequestExecutor;

    public SolrIndexValidationHealthProcessorPlugin(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, NodeService nodeService, SearchEndpointSelector<SolrEndpoint> solrServerSelector,
            SolrRequestExecutor solrRequestExecutor)
    {
        super(searchApplicationContextFactory, subsystemName);
        this.nodeService = nodeService;
        this.solrServerSelector = solrServerSelector;
        this.solrRequestExecutor = solrRequestExecutor;
    }

    protected Logger getLogger()
    {
        return log;
    }

    @Nonnull
    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs)
    {
        Set<NodeHealthReport> healthReports = new HashSet<>(nodeRefs.size());
        Map<NodeRef.Status, Set<NodeIndexHealthReport<SolrEndpoint>>> indexHealthReports = new HashMap<>(nodeRefs.size());

        // Collect node statuses
        Set<NodeRef.Status> nodeRefStatuses = nodeRefs.stream().map(nodeService::getNodeStatus).collect(Collectors.toSet());

        // Collect which search endpoints should contain which nodes
        Map<SolrEndpoint, Set<NodeRef.Status>> endpointToNodeMap = new HashMap<>();
        for (NodeRef.Status nodeRefStatus : nodeRefStatuses)
        {
            Set<SolrEndpoint> searchEndpoints = solrServerSelector.getSearchEndpointsForNode(nodeRefStatus);
            getLogger().trace("Found endpoints for node {}: {}", nodeRefStatus.getNodeRef(), searchEndpoints);
            for (SolrEndpoint searchEndpoint : searchEndpoints)
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

        // Perform searches on the endpoints that should contain a node
        for (Map.Entry<SolrEndpoint, Set<NodeRef.Status>> entry : endpointToNodeMap.entrySet())
        {
            SolrEndpoint searchEndpoint = entry.getKey();
            Set<NodeRef.Status> expectedNodeRefStatuses = new HashSet<>(entry.getValue());
            try
            {
                SolrSearchResult searchResult = solrRequestExecutor.checkNodeIndexed(searchEndpoint, expectedNodeRefStatuses);

                getLogger().trace("Search endpoint {}: expected nodes {}, result {}", searchEndpoint, expectedNodeRefStatuses,
                        searchResult);

                for (Status status : searchResult.getFound())
                {
                    indexHealthReports.get(status).add(new NodeIndexHealthReport<>(FOUND, status, searchEndpoint));
                }

                for (Status status : searchResult.getMissing())
                {
                    indexHealthReports.get(status).add(new NodeIndexHealthReport<>(NOT_FOUND, status, searchEndpoint));
                }

                for (Status status : searchResult.getNotIndexed())
                {
                    indexHealthReports.get(status).add(new NodeIndexHealthReport<>(NOT_INDEXED, status, searchEndpoint));
                }

                for (Status status : searchResult.getDuplicate())
                {
                    indexHealthReports.get(status).add(new NodeIndexHealthReport<>(DUPLICATE, status, searchEndpoint));
                }
            }
            catch (IOException exception)
            {
                getLogger().error("Exception during healthcheck on search endpoint {}", searchEndpoint, exception);
                for (Status nodeRefStatus : expectedNodeRefStatuses)
                {
                    indexHealthReports.get(nodeRefStatus).add(new NodeIndexHealthReport<>(EXCEPTION, nodeRefStatus, searchEndpoint));
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
        }).forEach(healthReports::add);

        return healthReports;
    }

    @Override
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("enabled", Boolean.toString(isEnabled()));
        configuration.put("subsystemName", subsystemName);
        configuration.put("solrServerSelector", solrServerSelector.toString());
        return configuration;
    }
}
