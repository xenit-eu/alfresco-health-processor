package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport.EndpointHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.Set;

public class SolrMissingNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {

    public SolrMissingNodeFixerPlugin(SolrSearchExecutor solrSearchExecutor) {
        super(solrSearchExecutor);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            EndpointHealthReport endpointHealthReport) {
        if(endpointHealthReport.getHealthStatus() != EndpointHealthStatus.NOT_FOUND) {
            return Collections.emptySet();
        }
        return Collections.singleton(
                trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX));
    }

}
