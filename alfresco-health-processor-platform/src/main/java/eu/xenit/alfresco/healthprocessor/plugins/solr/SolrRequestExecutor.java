package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.fasterxml.jackson.databind.JsonNode;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Performs HTTP requests on a {@link SearchEndpoint}
 */
@Slf4j
@RequiredArgsConstructor
public class SolrRequestExecutor {

    private final HttpClient httpClient;
    private final boolean checkTransaction;

    public SolrRequestExecutor(Boolean checkTransaction, Properties globalProperties) {
        this(SslHttpClientFactory.setupHttpClient(globalProperties), checkTransaction);
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
        String solrQuery;
        if (!checkTransaction) {
            solrQuery = nodeStatuses.stream()
                    .map(Status::getDbId)
                    .map(dbId -> "DBID:" + dbId)
                    .collect(Collectors.joining("%20OR%20"));
        } else {
            // FROM SS 2.0 Documents in SOLR also contain their related transaction (called INTXID).
            // Searching for both DBID and TX from Alfresco validates that the node is indexed
            // and that it's related transaction is the latest. (making sure no later transaction was accidentally skipped)
            solrQuery = nodeStatuses.stream()
                    .map(status -> "(DBID:" + status.getDbId() + "%20AND%20INTXID:" + status.getDbTxnId() + ")" )
                    .collect(Collectors.joining("%20OR%20"));
        }

        log.debug("Search query to endpoint {}: {}", endpoint, solrQuery);

        HttpUriRequest searchRequest = new HttpGet(
                endpoint.getBaseUri()
                        .resolve("select?q=" + solrQuery + "&fl=DBID&wt=json&rows=" + fetchSize));

        log.trace("Executing HTTP request {}", searchRequest);
        return httpClient.execute(searchRequest, new JSONResponseHandler());
    }

    /**
     * Schedules an async SolrNodeCommand for a node on a search endpoint.
     * This action/command is scheduled for execution by solr or a failure is returned.
     * @param endpoint the search endpoint
     * @param nodeStatus node status containing information about the dbIDs and transactionIds
     * @param command Solr action that will be executed
     * @return
     * @throws IOException when the command can not be sent to solr
     */
    public SolrActionResponse executeAsyncNodeCommand(SearchEndpoint endpoint, Status nodeStatus, SolrNodeCommand command)
            throws IOException {
        String coreName = endpoint.getCoreName();

        String solrEndpoint = "cores?action=" + command.getCommand() + "&wt=json&coreName=" + coreName;
        solrEndpoint += (command.isTargetsTransaction())? "&txid=" + nodeStatus.getDbTxnId() : "&nodeid=" + nodeStatus.getDbId();

        HttpUriRequest indexRequest = new HttpGet(endpoint.getAdminUri().resolve(solrEndpoint));

        log.trace("Executing HTTP request {}", indexRequest);
        JsonNode response = httpClient.execute(indexRequest, new JSONResponseHandler());
        log.trace("Response: {}", response.asText());
        return parseActionWebResponse(response, coreName);
    }

    private SolrActionResponse parseActionWebResponse(JsonNode response, String coreName) {
        String message;
        boolean successFull = response.path("responseHeader").path("status").asInt() == 0;
        if (!successFull) {
            message = response.path("error").path("msg").asText();
        } else {
            message = (response.has("action")) ?
                    response.path("action").path(coreName).path("status").asText() : "scheduled";
            successFull = message.equals("scheduled");
        }
        return new SolrActionResponse(successFull, message);
    }

    @Value
    public static class SolrActionResponse {
        private final boolean successFull;
        private final String message;
    }

    /**
     * The boolean targetsTransaction indicates if the action should be sent for the transaction the node was contained in.
     * If true, the nodeCommand will be scheduled for the complete transaction of this node.
     * If false, the nodeCommand is scheduled for this single node contained in the nodestatus.
     */
    @AllArgsConstructor
    public enum SolrNodeCommand {
        REINDEX("reindex", false),
        PURGE("purge", false),
        REINDEX_TRANSACTION("reindex", true);

        @Getter
        private final String command;
        @Getter
        private final boolean targetsTransaction;
    }

}
