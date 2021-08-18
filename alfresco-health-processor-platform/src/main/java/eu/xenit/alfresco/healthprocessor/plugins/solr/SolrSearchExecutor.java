package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.HttpEntity;
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
        JsonNode response = httpClient.execute(searchRequest, new JSONResponseHandler());

        Long lastIndexedTransaction = response.path("lastIndexedTx").asLong();

        Set<Long> foundDbIds = StreamSupport.stream(response.path("response").path("docs").spliterator(), false)
                .filter(JsonNode::isObject)
                .map(o -> o.path("DBID").asLong())
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

    private static class JSONResponseHandler extends AbstractResponseHandler<JsonNode> {

        @Override
        public JsonNode handleEntity(HttpEntity entity) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(entity.getContent());
        }
    }

}
