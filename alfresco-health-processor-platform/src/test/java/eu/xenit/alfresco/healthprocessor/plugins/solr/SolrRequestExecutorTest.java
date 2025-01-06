package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.github.paweladamski.httpclientmock.HttpClientMock;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrActionResponse;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.util.SetUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolrRequestExecutorTest {

    private HttpClientMock httpClientMock;
    private SolrRequestExecutor solrRequestExecutor;
    private SolrRequestExecutor solrRequestExecutorTransactionChecker;

    private static final long LAST_INDEXED_TX = 1000L;

    @BeforeEach
    void setup() {
        httpClientMock = new HttpClientMock();
        solrRequestExecutor = new SolrRequestExecutor(httpClientMock, false);
        solrRequestExecutorTransactionChecker = new SolrRequestExecutor(httpClientMock, true);
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
    void checkIndexedNodesAndTransactions() throws IOException {
        /*
        * In this case two nodes are contained in tx (txId 2) but one of them failed to index. (however the transaction is still considered as indexed)
        * */

        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, 1L));
        nodeRefs.add(randomNodeRefStatus(11L, 2L));
        nodeRefs.add(randomNodeRefStatus(20L,2L));
        nodeRefs.add(randomNodeRefStatus(100L, 5L));
        nodeRefs.add(randomNodeRefStatus(1000L, LAST_INDEXED_TX + 1));
        String q = "(DBID:10 AND INTXID:1) OR (DBID:11 AND INTXID:2) OR (DBID:20 AND INTXID:2) OR (DBID:100 AND INTXID:5) OR (DBID:1000 AND INTXID:" + (LAST_INDEXED_TX + 1) + ")";
        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("q", q)
                .withParameter("fl", "DBID")
                .withParameter("wt", "json")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 2, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutorTransactionChecker.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(100L, 20L), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(1000L), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getDuplicate()));
    }

    @Test
    void checkAllTransactionsIndexed() throws IOException {
        /*
            This case show the purpose of the solrRequestExecutorTransactionChecker.
            TRANSACTIONS:
             - 1L : Node 10 is created
             - 2L : Node 11 is created
             - 3L : Node 10 is updated -> Missing in SOLR

             Searching for DBID:10 indicates no problem with the solr index...
             Searching for DBID:10 in conjuction with INTXID:3 indicates a problem and reindexes TX 3
         */
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://nowhere/solr/index/"));

        List<Status> nodeRefs = new ArrayList<>();

        nodeRefs.add(randomNodeRefStatus(10L, 3L));
        nodeRefs.add(randomNodeRefStatus(11L, 2L));

        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("q", "DBID:10 OR DBID:11")
                .withParameter("fl", "DBID")
                .withParameter("wt", "json")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 2, \"docs\": ["
                        + "{\"DBID\": 10 },"
                        + "{\"DBID\": 11 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchResult = solrRequestExecutor.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(10L, 11L), toDbIds(solrSearchResult.getFound()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchResult.getDuplicate()));


        httpClientMock.onGet("http://nowhere/solr/index/select")
                .withParameter("q", "(DBID:10 AND INTXID:3) OR (DBID:11 AND INTXID:2)")
                .withParameter("fl", "DBID")
                .withParameter("wt", "json")
                .doReturnJSON("{"
                        + "\"lastIndexedTx\":" + LAST_INDEXED_TX + ","
                        + "\"response\": { \"numFound\": 1, \"docs\": ["
                        + "{\"DBID\": 11 }"
                        + "]}"
                        + "}");

        SolrSearchResult solrSearchTransactionalResult = solrRequestExecutorTransactionChecker.checkNodeIndexed(endpoint, nodeRefs);

        assertEquals(SetUtil.set(11L), toDbIds(solrSearchTransactionalResult.getFound()));
        assertEquals(SetUtil.set(10L), toDbIds(solrSearchTransactionalResult.getMissing()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchTransactionalResult.getNotIndexed()));
        assertEquals(SetUtil.set(), toDbIds(solrSearchTransactionalResult.getDuplicate()));

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

        httpClientMock.onGet("http://nowhere/solr/admin/cores?action=purge&nodeid=25&wt=json&coreName=index")
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
