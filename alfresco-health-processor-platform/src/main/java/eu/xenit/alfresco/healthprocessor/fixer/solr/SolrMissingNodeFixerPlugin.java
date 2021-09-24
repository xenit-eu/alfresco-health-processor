package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class SolrMissingNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {
    private HashMap<Long, NodeFixReport> reindexedTxMap = new HashMap<>();

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
        if (reindexedTxMap.containsKey(nodeStatus.getDbTxnId()) && reindexedTxMap.get(nodeStatus.getDbTxnId()).getFixStatus() == NodeFixStatus.SUCCEEDED) {
            //If a successful reindex action was already sent for this tx, do not schedule another one
            NodeFixReport cachedNodeFixReport = reindexedTxMap.get(nodeStatus.getDbTxnId());
            return Collections.singleton(new NodeFixReport(cachedNodeFixReport.getFixStatus(), unhealthyReport,
                    cachedNodeFixReport.getMessages()));
        } else {
            NodeFixReport nodeFixReport = trySendSolrCommand(unhealthyReport, endpointHealthReport,
                    SolrNodeCommand.REINDEX, true);
            reindexedTxMap.put(nodeStatus.getDbTxnId(), nodeFixReport);
            return Collections.singleton(nodeFixReport);
        }
    }

    @Override
    protected void clearCache() {
        reindexedTxMap.clear();
    }

}
