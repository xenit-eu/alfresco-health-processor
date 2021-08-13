package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SolrServerEndpointSelector;
import eu.xenit.alfresco.healthprocessor.plugins.solr.filter.SolrNodeFilter;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.slf4j.Logger;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class SolrIndexValidationHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final NodeService nodeService;
    private final SolrServerEndpointSelector solrServerSelector;
    private final SolrSearchExecutor solrSearchExecutor;

    private static final String NO_SEARCH_ENDPOINTS = "Node is not expected in any search index.";
    private static final String FOUND_FMT = "Node is present in search index %s.";
    private static final String NOT_FOUND_FMT = "Node is missing in search index %s.";
    private static final String NOT_INDEXED_TX_FMT = "Node is not yet indexed in search index %s (TX not yet processed).";
    private static final String EXCEPTION_FMT = "Exception occurred while checking node in search index %s.";

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        Map<NodeRef.Status, MutableHealthReport> healthReports = new HashMap<>(nodeRefs.size());

        // Collect node statuses
        Set<NodeRef.Status> nodeRefStatuses = nodeRefs.stream()
                .map(nodeService::getNodeStatus)
                .collect(Collectors.toSet());

        // Collect which search endpoints should contain which nodes
        Map<SearchEndpoint, Set<NodeRef.Status>> endpointToNodeMap = new HashMap<>();
        for (NodeRef.Status nodeRefStatus : nodeRefStatuses) {
            Set<SearchEndpoint> searchEndpoints = solrServerSelector.getSearchEndpointsForNode(nodeRefStatus);
            getLogger().trace("Found endpoints for node {}: {}", nodeRefStatus.getNodeRef(), searchEndpoints);
            for (SearchEndpoint searchEndpoint : searchEndpoints) {
                endpointToNodeMap.computeIfAbsent(searchEndpoint, k -> new HashSet<>()).add(nodeRefStatus);
            }
            if (searchEndpoints.isEmpty()) {
                getLogger().debug("Node {} has no search endpoints", nodeRefStatus.getNodeRef());
                healthReports.put(
                        nodeRefStatus,
                        new MutableHealthReport(NodeHealthStatus.NONE, nodeRefStatus.getNodeRef(), NO_SEARCH_ENDPOINTS)
                );
            } else {
                // Pre-allocate reports for other nodes (will be overwritten as appropriate)
                healthReports.put(
                        nodeRefStatus,
                        new MutableHealthReport(nodeRefStatus.getNodeRef())
                );
            }
        }

        // Perform searches on the endpoints that should contain a node
        for (Map.Entry<SearchEndpoint, Set<NodeRef.Status>> entry : endpointToNodeMap.entrySet()) {
            SearchEndpoint searchEndpoint = entry.getKey();
            Set<NodeRef.Status> expectedNodeRefStatuses = new HashSet<>(entry.getValue());
            try {
                SolrSearchResult searchResult = solrSearchExecutor.checkNodeIndexed(searchEndpoint,
                        expectedNodeRefStatuses);

                getLogger().trace("Search endpoint {}: expected nodes {}, result {}", searchEndpoint,
                        expectedNodeRefStatuses,
                        searchResult);

                for (Status status : searchResult.getFound()) {
                    healthReports.get(status).markHealthy(String.format(FOUND_FMT, searchEndpoint));
                }

                for (Status status : searchResult.getMissing()) {
                    healthReports.get(status).markUnhealthy(String.format(NOT_FOUND_FMT, searchEndpoint));
                }

                for (Status status : searchResult.getNotIndexed()) {
                    healthReports.get(status).markUnknown(String.format(NOT_INDEXED_TX_FMT, searchEndpoint));
                }
            } catch (IOException exception) {
                getLogger().error("Exception during healthcheck on search endpoint {}", searchEndpoint, exception);
                for (Status nodeRefStatus : expectedNodeRefStatuses) {
                    healthReports.get(nodeRefStatus).markUnknown(String.format(EXCEPTION_FMT, searchEndpoint));
                }
            }
        }

        return healthReports.values().stream()
                .map(MutableHealthReport::getHealthReport)
                .collect(Collectors.toSet());
    }
}
