package eu.xenit.alfresco.healthprocessor.plugins.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.paweladamski.httpclientmock.HttpClientMock;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrActionResponse;
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

class SolrRequestExecutorTest {

    private HttpClientMock httpClientMock;
    private SolrRequestExecutor solrRequestExecutor;

    private static final long LAST_INDEXED_TX = 1000L;

    @BeforeEach
    void setup() {
        httpClientMock = new HttpClientMock();
        solrRequestExecutor = new SolrRequestExecutor(httpClientMock);
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
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 3, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 },"
                        + "{\"DBID\": 999 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L, 999L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(100L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(1000L), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getDuplicate()));
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
                        + "\"response\": { \"numFound\": 5, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 },"
                        + "{\"DBID\": 999 },"
                        + "{\"DBID\": 1000 },"
                        + "{\"DBID\": 100 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L, 999L, 1000L, 100L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getDuplicate()));
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
            solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);
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
                        + "\"response\": { \"numFound\": 0, \"docs\": ["
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(10L, 11L, 999L, 100L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(1000L), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getDuplicate()));
    }

    @Test
    void checkDuplicateNodes() throws IOException {
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
                        + "\"response\": { \"numFound\": 5, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 999 },"
                        + "{\"DBID\": 1000 },"
                        + "{\"DBID\": 100 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(999L, 1000L, 100L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(11L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(10L), toDbIds(solrSearchResult.getDuplicate()));
    }

    @Test
    void checkManyDuplicateNodes() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, LAST_INDEXED_TX - 10));
        nodeRefs.add(randomNodeRefStatus(11L, LAST_INDEXED_TX - 9));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("rows", "4")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 6, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 }"
                        + "]}"
                        + "}");

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("rows", "6")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 6, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 },"
                        + "{\"DBID\": 10 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(11L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(10L), toDbIds(solrSearchResult.getDuplicate()));
    }

    @Test
    void performNodeCommandReindex() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 8L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=reindex&nodeid=25&wt=json&coreName=index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":0,"
                        + "\"QTime\":0 },"
                        + "\"action\": {"
                        + "\"index\": { \"status\": \"scheduled\" }"
                        + "}"
                        + "}");
        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.REINDEX);
        assertTrue(response.isSuccessFull());
        assertEquals("scheduled", response.getMessage());
    }

    //Solr version prior to 2.0 do not include the action response statuses in their response
    @Test
    void performNodeCommandReindexSolrv1x() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 8L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=reindex&nodeid=25&wt=json&coreName=index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":0,"
                        + "\"QTime\":0 }"
                        + "}");
        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.REINDEX);
        assertTrue(response.isSuccessFull());
        assertEquals("scheduled", response.getMessage());
    }

    @Test
    void performNodeCommandPurge() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/some-index/"));

        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 8L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=purge&nodeid=25&wt=json&coreName=some-index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":0,"
                        + "\"QTime\":0 },"
                        + "\"action\": {"
                        + "\"some-index\": { \"status\": \"scheduled\" }"
                        + "}"
                        + "}");

        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.PURGE);
        assertTrue(response.isSuccessFull());
        assertEquals("scheduled", response.getMessage());
    }

    @Test
    void performNodeCommandStatusCode500() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 8L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=purge&nodeid=25&wt=json&coreName=index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":500,"
                        + "\"QTime\":0 }"
                        + "}");

        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.PURGE);
        assertFalse(response.isSuccessFull());
    }

    @Test
    void performNodeCommandFails() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 8L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=purge&txid=8&wt=json&coreName=index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":0,"
                        + "\"QTime\":0 },"
                        + "\"action\": {"
                        + "\"index\": { \"status\": \"failed\" }"
                        + "}"
                        + "}");

        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.PURGE);
        assertFalse(response.isSuccessFull());
        assertEquals("failed", response.getMessage());
    }

    @Test
    void targetTransactionCommand() throws IOException {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/some-index/"));
        NodeRef.Status nodeRefStatus = randomNodeRefStatus(25L, 10L);

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=reindex&txid=10&wt=json&coreName=some-index")
                .doReturnJSON("{"
                        + "\"responseHeader\":{"
                        + "\"status\":0,"
                        + "\"QTime\":0 },"
                        + "\"action\": {"
                        + "\"some-index\": { \"status\": \"scheduled\" }"
                        + "}"
                        + "}");

        SolrActionResponse response = solrRequestExecutor.executeAsyncNodeCommand(endpoint,
                nodeRefStatus, SolrNodeCommand.REINDEX_TRANSACTION);
        assertTrue(response.isSuccessFull());
        assertEquals("scheduled", response.getMessage());
    }
}
