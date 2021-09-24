package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.Set;

public class SolrMissingNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {

    public SolrMissingNodeFixerPlugin(SolrRequestExecutor solrRequestExecutor) {
        super(solrRequestExecutor);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport endpointHealthReport) {
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.NOT_FOUND) {
            return Collections.emptySet();
        }
        return Collections.singleton(
                trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX, true));
    }

}
