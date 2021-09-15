package eu.xenit.alfresco.healthprocessor.fixer.solr;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrIndexValidationHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class SolrDuplicateNodeFixerPluginTest {

    private SolrRequestExecutor executor;
    private SolrDuplicateNodeFixerPlugin duplicateNodeFixerPlugin;

    @BeforeEach
    void setup() {
        executor = mock(SolrRequestExecutor.class);
        duplicateNodeFixerPlugin = new SolrDuplicateNodeFixerPlugin(executor);
    }

    @Test
    void fix_duplicate_nodes() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));
        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Arrays.asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        InOrder inOrder = Mockito.inOrder(executor);
        inOrder.verify(executor)
                .executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE);
        inOrder.verify(executor)
                .executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void fix_duplicate_nodes_multiple_endpoints() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint1 = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        SearchEndpoint endpoint2 = new SearchEndpoint(URI.create("http://empty/solr/core2/"));
        NodeRef.Status nodeRefStatus = new Status(123L, healthReport.getNodeRef(), "1", 1L, false);
        NodeIndexHealthReport nodeIndexHealthReport1 = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                nodeRefStatus,
                endpoint1
        );
        NodeIndexHealthReport nodeIndexHealthReport2 = new NodeIndexHealthReport(
                IndexHealthStatus.FOUND,
                nodeRefStatus,
                endpoint2
        );
        healthReport.data(NodeIndexHealthReport.class)
                .addAll(Arrays.asList(nodeIndexHealthReport1, nodeIndexHealthReport2));

        when(executor.executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.PURGE))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));
        when(executor.executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.REINDEX))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Arrays.asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        InOrder inOrder = Mockito.inOrder(executor);
        inOrder.verify(executor).executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.PURGE);
        inOrder.verify(executor).executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.REINDEX);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void fix_multiple_duplicate_nodes() throws IOException {
        NodeHealthReport healthReport1 = TestReports.unhealthy();
        NodeHealthReport healthReport2 = TestReports.unhealthy();
        SearchEndpoint endpoint1 = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeRef.Status nodeRefStatus1 = new Status(123L, healthReport1.getNodeRef(), "1", 1L, false);
        NodeRef.Status nodeRefStatus2 = new Status(1234L, healthReport2.getNodeRef(), "1", 1L, false);
        NodeIndexHealthReport nodeIndexHealthReport1 = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                nodeRefStatus1,
                endpoint1
        );
        NodeIndexHealthReport nodeIndexHealthReport2 = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                nodeRefStatus2,
                endpoint1
        );
        healthReport1.data(NodeIndexHealthReport.class).add(nodeIndexHealthReport1);
        healthReport2.data(NodeIndexHealthReport.class).add(nodeIndexHealthReport2);

        when(executor.executeAsyncNodeCommand(eq(endpoint1), any(), eq(SolrNodeCommand.PURGE)))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));
        when(executor.executeAsyncNodeCommand(eq(endpoint1), any(), eq(SolrNodeCommand.REINDEX)))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                set(healthReport1, healthReport2));

        assertEquals(Arrays.asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().filter(fixReport -> fixReport.getHealthReport().equals(healthReport1))
                        .map(NodeFixReport::getFixStatus).collect(
                                Collectors.toList()));
        assertEquals(Arrays.asList(NodeFixStatus.SUCCEEDED, NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().filter(fixReport -> fixReport.getHealthReport().equals(healthReport2))
                        .map(NodeFixReport::getFixStatus).collect(
                                Collectors.toList()));
    }

    @Test
    void fix_duplicate_nodes_fails_purge() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(false ,"failed"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Collections.singletonList(NodeFixStatus.FAILED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE);
        verifyNoMoreInteractions(executor);

    }

    @Test
    void fix_duplicate_nodes_fails_reindex() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));
        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(false ,"failed"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(set(NodeFixStatus.SUCCEEDED, NodeFixStatus.FAILED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toSet()));

        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE);
        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(),
                SolrNodeCommand.REINDEX);
        verifyNoMoreInteractions(executor);

    }

    @Test
    void fix_duplicate_nodes_throws_purge() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE))
                .thenThrow(new IOException("Something very bad happened"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Collections.singletonList(NodeFixStatus.FAILED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE);
        verifyNoMoreInteractions(executor);

    }

    @Test
    void fix_duplicate_nodes_throws_reindex() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.DUPLICATE,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE))
                .thenReturn(new SolrRequestExecutor.SolrActionResponse(true ,"scheduled"));
        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX))
                .thenThrow(new IOException("Something very bad happened"));

        Set<NodeFixReport> nodeFixReports = duplicateNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(set(NodeFixStatus.SUCCEEDED, NodeFixStatus.FAILED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toSet()));

        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.PURGE);
        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(),
                SolrNodeCommand.REINDEX);
        verifyNoMoreInteractions(executor);
    }
}
