package eu.xenit.alfresco.healthprocessor.plugins.solr;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport.EndpointHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolrIndexValidationHealthProcessorPluginTest {

    @Mock
    private NodeService nodeService;

    @Mock
    private SearchEndpointSelector searchEndpointSelector;

    @Mock
    private SolrSearchExecutor solrSearchExecutor;

    private SolrIndexValidationHealthProcessorPlugin healthProcessorPlugin;

    @BeforeEach
    void setup() {
        healthProcessorPlugin = new SolrIndexValidationHealthProcessorPlugin(nodeService, searchEndpointSelector,
                solrSearchExecutor);
    }

    @Test
    void process_single_endpoint_all_present() throws IOException {
        SearchEndpoint searchEndpoint = new SearchEndpoint(URI.create("http://empty/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                Collections.singleton(searchEndpoint));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "found" for this test
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that the search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.HEALTHY)), "Expect all nodes to be healthy");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(
                        Collections.singleton(
                                EndpointHealthStatus.FOUND.formatReason(searchEndpoint)))),
                "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_all_present() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "found" in endpoint 1 and endpoint 2
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.HEALTHY)), "Expect all nodes to be healthy");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.FOUND.formatReason(searchEndpoint1),
                EndpointHealthStatus.FOUND.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_some_missing() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "found" in endpoint 1 for this test and missing in endpoint 2
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), invocation.getArgument(1, Set.class),
                        Collections.emptySet(), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.UNHEALTHY)), "Expect all nodes to be unhealthy");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.FOUND.formatReason(searchEndpoint1),
                EndpointHealthStatus.NOT_FOUND.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_some_not_indexed() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "found" in endpoint 1 for this test and not indexed in endpoint 2
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), Collections.emptySet(),
                        invocation.getArgument(1, Set.class), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.HEALTHY)), "Expect all nodes to be healthy");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.FOUND.formatReason(searchEndpoint1),
                EndpointHealthStatus.NOT_INDEXED.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_all_not_indexed() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "found" in endpoint 1 for this test and not indexed in endpoint 2
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), Collections.emptySet(),
                        invocation.getArgument(1, Set.class), Collections.emptySet()));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), Collections.emptySet(),
                        invocation.getArgument(1, Set.class), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.NONE)), "Expect all nodes to be none");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.NOT_INDEXED.formatReason(searchEndpoint1),
                EndpointHealthStatus.NOT_INDEXED.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_some_duplicates() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All noderefs are regarded as "duplicate" in endpoint 1 for this test and not indexed in endpoint 2
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), Collections.emptySet(),
                        Collections.emptySet(), invocation.getArgument(1, Set.class)));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(Collections.emptySet(), Collections.emptySet(),
                        invocation.getArgument(1, Set.class), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.UNHEALTHY)), "Expect all nodes to be unhealthy");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.DUPLICATE.formatReason(searchEndpoint1),
                EndpointHealthStatus.NOT_INDEXED.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }


    @Test
    void process_no_endpoints() {
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(Collections.emptySet());

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that no search endpoints were contacted
        verifyNoInteractions(solrSearchExecutor);

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.NONE)), "Expect all nodes to be none");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                SolrIndexValidationHealthProcessorPlugin.MSG_NO_SEARCH_ENDPOINTS
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_multiple_endpoints_exception() throws IOException {
        SearchEndpoint searchEndpoint1 = new SearchEndpoint(URI.create("http://empty/solr/index1/"));
        SearchEndpoint searchEndpoint2 = new SearchEndpoint(URI.create("http://empty/solr/index2/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(
                set(searchEndpoint1, searchEndpoint2));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // All endpoint 1 throws an exception, endpoint 2 says they are present
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any())).thenThrow(
                new IOException("My server is broken"));
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any())).thenAnswer(
                invocation -> new SolrSearchResult(invocation.getArgument(1, Set.class), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet()));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that each search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint1), Mockito.any());
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint2), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.NONE)), "Expect all nodes to be none");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.EXCEPTION.formatReason(searchEndpoint1),
                EndpointHealthStatus.FOUND.formatReason(searchEndpoint2)
        ))), "Expect all nodes to have a message");
    }

    @Test
    void process_single_endpoint_http_error() throws IOException {
        SearchEndpoint searchEndpoint = new SearchEndpoint(URI.create("http://empty/solr/index/"));
        when(searchEndpointSelector.getSearchEndpointsForNode(Mockito.any())).thenReturn(set(searchEndpoint));

        when(nodeService.getNodeStatus(Mockito.any())).then(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Status(1L, nodeRef, "1", 1L, false);
        });

        // Endpoint throws an HTTP error
        when(solrSearchExecutor.checkNodeIndexed(Mockito.eq(searchEndpoint), Mockito.any())).thenThrow(
                new HttpResponseException(404, "Not Found"));

        Set<NodeRef> nodeRefs = set(TestNodeRefs.REFS);
        Set<NodeHealthReport> healthReports = healthProcessorPlugin.process(nodeRefs);

        // Verify that the search endpoint was only called *once*
        verify(solrSearchExecutor).checkNodeIndexed(Mockito.eq(searchEndpoint), Mockito.any());

        assertEquals(nodeRefs.size(), healthReports.size(),
                "Expected an equal number of health reports as the number of passed noderefs");
        assertTrue(healthReports.stream().map(NodeHealthReport::getStatus)
                .allMatch(Predicate.isEqual(NodeHealthStatus.NONE)), "Expect all nodes to be none");
        assertTrue(healthReports.stream().map(NodeHealthReport::getMessages).allMatch(Predicate.isEqual(set(
                EndpointHealthStatus.EXCEPTION.formatReason(searchEndpoint)
        ))), "Expect all nodes to have a message");
    }
}
