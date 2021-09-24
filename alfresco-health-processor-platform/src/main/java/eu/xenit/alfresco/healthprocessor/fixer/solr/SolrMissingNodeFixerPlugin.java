package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class SolrMissingNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {
    private HashMap<SearchEndpoint, HashMap<Long, NodeFixReport>> searchEndpointTxCache = new HashMap<>();

    public SolrMissingNodeFixerPlugin(SolrRequestExecutor solrRequestExecutor) {
        super(solrRequestExecutor);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport endpointHealthReport) {
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.NOT_FOUND) {
            return Collections.emptySet();
        }

        NodeRef.Status nodeStatus = endpointHealthReport.getNodeRefStatus();
        if (searchEndpointTxCache.containsKey(endpointHealthReport.getEndpoint())) {
            HashMap<Long, NodeFixReport> reindexTxCache = searchEndpointTxCache.get(endpointHealthReport.getEndpoint());
            if (reindexTxCache.containsKey(nodeStatus.getDbTxnId()) &&
                    reindexTxCache.get(nodeStatus.getDbTxnId()).getFixStatus() == NodeFixStatus.SUCCEEDED) {
                //If a successful reindex action was already sent for this tx to this endpoint, do not schedule another one
                NodeFixReport cachedNodeFixReport = reindexTxCache.get(nodeStatus.getDbTxnId());
                return Collections.singleton(new NodeFixReport(cachedNodeFixReport.getFixStatus(), unhealthyReport,
                        cachedNodeFixReport.getMessages()));
            }
        } else {
            searchEndpointTxCache.put(endpointHealthReport.getEndpoint(), new HashMap<>());
        }

        // Action not yet successfully send
        NodeFixReport nodeFixReport = trySendSolrCommand(unhealthyReport, endpointHealthReport,
                SolrNodeCommand.REINDEX_TRANSACTION);

        searchEndpointTxCache.get(endpointHealthReport.getEndpoint()).put(nodeStatus.getDbTxnId(), nodeFixReport);
        return Collections.singleton(nodeFixReport);
    }

    @Override
    protected void clearCache() {
        searchEndpointTxCache.clear();
    }

}
