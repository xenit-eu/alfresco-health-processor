package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.fasterxml.jackson.databind.JsonNode;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Performs HTTP requests on a {@link SearchEndpoint}
 */
@Slf4j
@RequiredArgsConstructor
public class SolrRequestExecutor {

    private final HttpClient httpClient;

    public SolrRequestExecutor() {
        this(HttpClientBuilder.create().build());
    }

    /**
     * Performs a search operation on an endpoint to determine if the nodes are indexed or not
     *
     * @param endpoint     The endpoint to perform a search on
     * @param nodeStatuses Nodes to search for
     * @return The result of the search operation
     * @throws IOException When the HTTP request goes wrong
     */
    public SolrSearchResult checkNodeIndexed(SearchEndpoint endpoint, Collection<Status> nodeStatuses)
            throws IOException {

        // Initially, try a fetch for double the size of the node statuses array
        // This is so we can immediately detect the case where all nodes are indexed twice.
        int fetchSize = nodeStatuses.size() * 2;
        JsonNode response = executeSearchRequest(endpoint, nodeStatuses, fetchSize);

        long numberOfFoundDocs = response.path("response").path("numFound").asLong();
        if (numberOfFoundDocs > fetchSize) {
            // We did not fetch enough in one batch to fetch all duplicates (when nodes are duplicated more than once)
            // Send a new request for the number of rows we actually need.
            log.debug(
                    "Found number of docs #{} is larger than the requested number of rows #{}. Fetching again with larger number of rows.",
                    numberOfFoundDocs, fetchSize);
            response = executeSearchRequest(endpoint, nodeStatuses, numberOfFoundDocs);
        }

        Long lastIndexedTransaction = response.path("lastIndexedTx").asLong();

        JsonNode docs = response.path("response").path("docs");

        // Map from DBID to number of times that it is present
        Map<Long, Long> foundDbIds = StreamSupport.stream(docs.spliterator(), false)
                .filter(JsonNode::isObject)
                .map(o -> o.path("DBID").asLong())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        log.debug("Last indexed transaction in solr: {}", lastIndexedTransaction);
        if (log.isTraceEnabled()) {
            log.trace("Transactions on nodes: {}",
                    nodeStatuses.stream().map(Status::getDbTxnId).collect(Collectors.toSet()));
        }

        SolrSearchResult solrSearchResult = new SolrSearchResult();

        for (Status nodeStatus : nodeStatuses) {
            switch (foundDbIds.getOrDefault(nodeStatus.getDbId(), 0L).intValue()) {
                case 0:
                    // Node is in a transaction that has not yet been indexed
                    if (nodeStatus.getDbTxnId() > lastIndexedTransaction) {
                        log.trace("Node {} is not yet indexed (solr indexed TX: {})", nodeStatus,
                                lastIndexedTransaction);
                        solrSearchResult.getNotIndexed().add(nodeStatus);
                    } else {
                        log.trace("Node {} is not indexed (solr indexed TX: {})", nodeStatus, lastIndexedTransaction);
                        solrSearchResult.getMissing().add(nodeStatus);
                    }
                    break;
                case 1:
                    solrSearchResult.getFound().add(nodeStatus);
                    break;
                default:
                    log.trace("Node {} is indexed multiple times (found {} times)", nodeStatus,
                            foundDbIds.get(nodeStatus.getDbId()));
                    solrSearchResult.getDuplicate().add(nodeStatus);
            }
        }

        return solrSearchResult;
    }

    private JsonNode executeSearchRequest(SearchEndpoint endpoint, Collection<Status> nodeStatuses, long fetchSize)
            throws IOException {
        String dbIdsQuery = nodeStatuses.stream()
                .map(Status::getDbId)
                .map(dbId -> "DBID:" + dbId)
                .collect(Collectors.joining("%20OR%20"));

        log.debug("Search query to endpoint {}: {}", endpoint, dbIdsQuery);

        HttpUriRequest searchRequest = new HttpGet(
                endpoint.getBaseUri()
                        .resolve("select?q=" + dbIdsQuery + "&fl=DBID&wt=json&rows=" + fetchSize));

        log.trace("Executing HTTP request {}", searchRequest);
        return httpClient.execute(searchRequest, new JSONResponseHandler());
    }

    public boolean executeAsyncNodeCommand(SearchEndpoint endpoint, Status nodeStatus, SolrNodeCommand command)
            throws IOException {
        String coreName = endpoint.getCoreName();
        HttpUriRequest indexRequest = new HttpGet(endpoint.getAdminUri().resolve(
                "cores?action=" + command.getCommand() + "&nodeid=" + nodeStatus.getDbId() + "&wt=json&coreName="
                        + coreName));

        log.trace("Executing HTTP request {}", indexRequest);
        JsonNode response = httpClient.execute(indexRequest, new JSONResponseHandler());
        log.trace("Response: {}", response.asText());

        return response.path("action").path(coreName).path("status").asText().equals("scheduled");
    }


    @AllArgsConstructor
    public enum SolrNodeCommand {
        REINDEX("reindex"),
        PURGE("purge");

        @Getter
        private final String command;
    }

}
