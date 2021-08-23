package eu.xenit.alfresco.healthprocessor.plugins.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.paweladamski.httpclientmock.HttpClientMock;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.util.SetUtil;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolrSearchExecutorTest {

    private HttpClientMock httpClientMock;
    private SolrSearchExecutor solrSearchExecutor;

    private static final long LAST_INDEXED_TX = 1000L;

    @BeforeEach
    void setup() {
        httpClientMock = new HttpClientMock();
        solrSearchExecutor = new SolrSearchExecutor(httpClientMock);
    }

    private static NodeRef randomNodeRef() {
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
    }

    private static Status randomNodeRefStatus(Long dbId, Long txId) {
        return new Status(dbId, randomNodeRef(), txId.toString(), txId, false);
    }

    private static Set<Long> toDbIds(Set<Status> nodeRefs) {
        return nodeRefs.stream().map(Status::getDbId).collect(Collectors.toSet());
    }

    @Test
    void checkIndexedNodes() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, LAST_INDEXED_TX - 10));
        nodeRefs.add(randomNodeRefStatus(11L, LAST_INDEXED_TX - 9));
        nodeRefs.add(randomNodeRefStatus(100L, 5L));
        nodeRefs.add(randomNodeRefStatus(999L, LAST_INDEXED_TX));
        nodeRefs.add(randomNodeRefStatus(1000L, LAST_INDEXED_TX + 1));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("q", "DBID:10 OR DBID:11 OR DBID:100 OR DBID:999 OR DBID:1000")
                .withParameter("fl", "DBID")
                .withParameter("wt", "json")
                .withParameter("rows", "5")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 },"
                        + "{\"DBID\": 999 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrSearchExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L, 999L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(100L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(1000L), toDbIds(solrSearchResult.getNotIndexed()));
    }

    @Test
    void checkIndexedNodesAllIndexed() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, LAST_INDEXED_TX - 10));
        nodeRefs.add(randomNodeRefStatus(11L, LAST_INDEXED_TX - 9));
        nodeRefs.add(randomNodeRefStatus(100L, 5L));
        nodeRefs.add(randomNodeRefStatus(999L, LAST_INDEXED_TX));
        nodeRefs.add(randomNodeRefStatus(1000L, LAST_INDEXED_TX));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 },"
                        + "{\"DBID\": 999 },"
                        + "{\"DBID\": 1000 },"
                        + "{\"DBID\": 100 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrSearchExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L, 999L, 1000L, 100L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getNotIndexed()));
    }

    @Test
    void checkSolrHttpError() {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();
        nodeRefs.add(randomNodeRefStatus(10L, LAST_INDEXED_TX - 10));
        nodeRefs.add(randomNodeRefStatus(11L, LAST_INDEXED_TX - 9));
        nodeRefs.add(randomNodeRefStatus(100L, 5L));
        nodeRefs.add(randomNodeRefStatus(999L, LAST_INDEXED_TX));
        nodeRefs.add(randomNodeRefStatus(1000L, LAST_INDEXED_TX + 1));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .doReturnWithStatus(400);

        assertThrows(HttpResponseException.class, () -> {
            solrSearchExecutor.checkNodeIndexed(endpoint, nodeRefs);
        });
    }

    @Test
    void checkEmptyResponse() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, LAST_INDEXED_TX - 10));
        nodeRefs.add(randomNodeRefStatus(11L, LAST_INDEXED_TX - 9));
        nodeRefs.add(randomNodeRefStatus(100L, 5L));
        nodeRefs.add(randomNodeRefStatus(999L, LAST_INDEXED_TX));
        nodeRefs.add(randomNodeRefStatus(1000L, LAST_INDEXED_TX + 1));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"docs\": ["
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrSearchExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(10L, 11L, 999L, 100L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(1000L), toDbIds(solrSearchResult.getNotIndexed()));
    }
}
