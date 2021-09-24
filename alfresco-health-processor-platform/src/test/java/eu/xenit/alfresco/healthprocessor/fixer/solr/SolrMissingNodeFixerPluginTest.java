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
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrActionResponse;
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

class SolrMissingNodeFixerPluginTest {

    private SolrRequestExecutor executor;
    private SolrMissingNodeFixerPlugin missingNodeFixerPlugin;

    @BeforeEach
    void setup() {
        executor = mock(SolrRequestExecutor.class);
        missingNodeFixerPlugin = new SolrMissingNodeFixerPlugin(executor);
    }

    @Test
    void fix_missing_nodes() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX_TRANSACTION)).thenReturn(new SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = missingNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Collections.singletonList(NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        verify(executor).executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(),
                SolrNodeCommand.REINDEX_TRANSACTION);
        verifyNoMoreInteractions(executor);
    }

    @Test
    void fix_missing_nodes_multiple_endpoints() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint1 = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        SearchEndpoint endpoint2 = new SearchEndpoint(URI.create("http://empty/solr/core2/"));
        NodeRef.Status nodeRefStatus = new Status(123L, healthReport.getNodeRef(), "1", 1L, false);
        NodeIndexHealthReport nodeIndexHealthReport1 = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                nodeRefStatus,
                endpoint1
        );
        NodeIndexHealthReport nodeIndexHealthReport2 = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_INDEXED,
                nodeRefStatus,
                endpoint2
        );
        healthReport.data(NodeIndexHealthReport.class)
                .addAll(Arrays.asList(nodeIndexHealthReport1, nodeIndexHealthReport2));

        when(executor.executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.REINDEX_TRANSACTION))
                .thenReturn(new SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = missingNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(Collections.singletonList(NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                        .collect(Collectors.toList()));

        verify(executor).executeAsyncNodeCommand(endpoint1, nodeRefStatus, SolrNodeCommand.REINDEX_TRANSACTION);
        verifyNoMoreInteractions(executor);
    }

    @Test
    void fix_multiple_missing_nodes() throws IOException {
        NodeHealthReport healthReport1 = TestReports.unhealthy();
        NodeHealthReport healthReport2 = TestReports.unhealthy();
        SearchEndpoint endpoint1 = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeRef.Status nodeRefStatus1 = new Status(123L, healthReport1.getNodeRef(), "1", 1L, false);
        NodeRef.Status nodeRefStatus2 = new Status(1234L, healthReport2.getNodeRef(), "1", 1L, false);
        NodeIndexHealthReport nodeIndexHealthReport1 = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                nodeRefStatus1,
                endpoint1
        );
        NodeIndexHealthReport nodeIndexHealthReport2 = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                nodeRefStatus2,
                endpoint1
        );
        healthReport1.data(NodeIndexHealthReport.class).add(nodeIndexHealthReport1);
        healthReport2.data(NodeIndexHealthReport.class).add(nodeIndexHealthReport2);

        when(executor.executeAsyncNodeCommand(eq(endpoint1), any(), eq(SolrNodeCommand.REINDEX_TRANSACTION)))
                .thenReturn(new SolrActionResponse(true ,"scheduled"));

        Set<NodeFixReport> nodeFixReports = missingNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                set(healthReport1, healthReport2));

        assertEquals(Collections.singletonList(NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().filter(fixReport -> fixReport.getHealthReport().equals(healthReport1))
                        .map(NodeFixReport::getFixStatus).collect(
                                Collectors.toList()));
        assertEquals(Collections.singletonList(NodeFixStatus.SUCCEEDED),
                nodeFixReports.stream().filter(fixReport -> fixReport.getHealthReport().equals(healthReport2))
                        .map(NodeFixReport::getFixStatus).collect(
                                Collectors.toList()));
    }

    @Test
    void fix_missing_nodes_fails_reindex() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(),
                SolrNodeCommand.REINDEX_TRANSACTION))
                .thenReturn(new SolrActionResponse(false ,"error"));

        Set<NodeFixReport> nodeFixReports = missingNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(set(NodeFixStatus.FAILED), nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                .collect(Collectors.toSet()));

    }

    @Test
    void fix_missing_nodes_throws_reindex() throws IOException {
        NodeHealthReport healthReport = TestReports.unhealthy();
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/solr/core1/"));
        NodeIndexHealthReport nodeIndexHealthReport = new NodeIndexHealthReport(
                IndexHealthStatus.NOT_FOUND,
                new Status(123L, healthReport.getNodeRef(), "1", 1L, false),
                endpoint
        );
        healthReport.data(NodeIndexHealthReport.class)
                .add(nodeIndexHealthReport);

        when(executor.executeAsyncNodeCommand(endpoint, nodeIndexHealthReport.getNodeRefStatus(),
                SolrNodeCommand.REINDEX_TRANSACTION))
                .thenThrow(new IOException("Something very bad happened"));

        Set<NodeFixReport> nodeFixReports = missingNodeFixerPlugin.fix(SolrIndexValidationHealthProcessorPlugin.class,
                Collections.singleton(healthReport));

        assertEquals(set(NodeFixStatus.FAILED), nodeFixReports.stream().map(NodeFixReport::getFixStatus)
                .collect(Collectors.toSet()));
    }

}
