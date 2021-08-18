package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

/**
 * Performs a search operation on a {@link SearchEndpoint}
 */
@Slf4j
@RequiredArgsConstructor
public class SolrSearchExecutor {

    private final HttpClient httpClient;

    public SolrSearchExecutor() {
        httpClient = HttpClientBuilder.create().build();
    }

    /**
     * Performs a search operation on an endpoint to determine if the nodes are indexed or not
     *
     * @param endpoint     The endpoint to perform a search on
     * @param nodeStatuses Nodes to search for
     * @return The result of the search operation
     * @throws IOException When the HTTP request goes wrong
     */
    public SolrSearchResult checkNodeIndexed(SearchEndpoint endpoint, Set<NodeRef.Status> nodeStatuses)
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
        String response = httpClient.execute(searchRequest, new BasicResponseHandler());

        JSONObject responseJson = new JSONObject(response);

        Long lastIndexedTransaction = responseJson.getLong("lastIndexedTx");

        Set<Long> foundDbIds = StreamSupport.stream(
                        responseJson.getJSONObject("response").getJSONArray("docs").spliterator(), false)
                .filter(o -> o instanceof JSONObject)
                .map(o -> (JSONObject) o)
                .map(o -> o.getLong("DBID"))
                .collect(Collectors.toSet());

        SolrSearchResult solrSearchResult = new SolrSearchResult();

        for (Status nodeStatus : nodeStatuses) {
            // Node is in a transaction that has not yet been indexed
            if (nodeStatus.getDbTxnId() > lastIndexedTransaction) {
                log.trace("Node {} is not yet indexed (solr indexed TX: {})", nodeStatus, lastIndexedTransaction);
                solrSearchResult.getNotIndexed().add(nodeStatus);
            } else if (foundDbIds.contains(nodeStatus.getDbId())) {
                solrSearchResult.getFound().add(nodeStatus);
            } else {
                solrSearchResult.getMissing().add(nodeStatus);
            }

        }

        return solrSearchResult;
    }


}
