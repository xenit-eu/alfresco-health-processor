package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.fasterxml.jackson.databind.JsonNode;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Performs a search operation on a {@link SearchEndpoint}
 */
@Slf4j
@RequiredArgsConstructor
public class SolrSearchExecutor {

    private final HttpClient httpClient;

    public SolrSearchExecutor() {
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

        String dbIdsQuery = nodeStatuses.stream()
                .map(Status::getDbId)
                .map(dbId -> "DBID:" + dbId)
                .collect(Collectors.joining("%20OR%20"));

        log.debug("Search query to endpoint {}: {}", endpoint, dbIdsQuery);

        HttpUriRequest searchRequest = new HttpGet(
                endpoint.getBaseUri()
                        .resolve("select?q=" + dbIdsQuery + "&fl=DBID&wt=json&rows=" + nodeStatuses.size()));

        log.trace("Executing HTTP request {}", searchRequest);
        JsonNode response = httpClient.execute(searchRequest, new JSONResponseHandler());

        Long lastIndexedTransaction = response.path("lastIndexedTx").asLong();

        JsonNode docs = response.path("response").path("docs");
        if(docs.size() == nodeStatuses.size()) {
            log.debug("Received #{} docs (identical to requested #{} nodes), marking all as found.", docs.size(), nodeStatuses.size());
            // Fast path: all searched for nodes were found.
            return new SolrSearchResult(new HashSet<>(nodeStatuses), Collections.emptySet(), Collections.emptySet());
        }

        Set<Long> foundDbIds = StreamSupport.stream(docs.spliterator(), false)
                .filter(JsonNode::isObject)
                .map(o -> o.path("DBID").asLong())
                .collect(Collectors.toSet());

        log.debug("Last indexed transaction in solr: {}", lastIndexedTransaction);
        if(log.isTraceEnabled()) {
            log.trace("Transactions on nodes: {}", nodeStatuses.stream().map(Status::getDbTxnId).collect(Collectors.toSet()));
        }

        SolrSearchResult solrSearchResult = new SolrSearchResult();

        for (Status nodeStatus : nodeStatuses) {
            // Node is in a transaction that has not yet been indexed
            if (nodeStatus.getDbTxnId() > lastIndexedTransaction) {
                log.trace("Node {} is not yet indexed (solr indexed TX: {})", nodeStatus, lastIndexedTransaction);
                solrSearchResult.getNotIndexed().add(nodeStatus);
            } else if (foundDbIds.contains(nodeStatus.getDbId())) {
                solrSearchResult.getFound().add(nodeStatus);
            } else {
                log.trace("Node {} is not indexed (solr indexed TX: {})", nodeStatus, lastIndexedTransaction);
                solrSearchResult.getMissing().add(nodeStatus);
            }

        }

        return solrSearchResult;
    }

    public boolean forceNodeIndex(SearchEndpoint endpoint, Status nodeStatus) throws IOException {
        String coreName = endpoint.getCoreName();
        HttpUriRequest indexRequest = new HttpGet(endpoint.getAdminUri().resolve("cores?action=index&nodeid="+nodeStatus.getDbId()+"&wt=json&coreName="+coreName));

        log.trace("Executing HTTP request {}", indexRequest);
        JsonNode response = httpClient.execute(indexRequest, new JSONResponseHandler());
        log.trace("Response: {}", response.asText());

        return response.path("action").path(coreName).path("status").asText().equals("scheduled");
    }

}
