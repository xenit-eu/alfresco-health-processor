package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport.EndpointHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SolrDuplicateNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {

    public SolrDuplicateNodeFixerPlugin(SolrSearchExecutor solrSearchExecutor) {
        super(solrSearchExecutor);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            EndpointHealthReport endpointHealthReport) {
        if(endpointHealthReport.getHealthStatus() != EndpointHealthStatus.DUPLICATE) {
            return Collections.emptySet();
        }
        // When a duplicate node is detected, purge it from the index and reindex it
        // According to MetadataTracker#maintenance(), purge is processed before reindex
        // Even if it is executed in the same maintenance cycle.
        // Ref: https://github.com/Alfresco/SearchServices/blob/e7f05e2f13a709cd28afa3ae6acfd3d0000b22ff/search-services/alfresco-search/src/main/java/org/alfresco/solr/tracker/MetadataTracker.java#L257-L266
        // Purge has to be done before reindex, else we end up with a broken index which will only be fixed
        // by a subsequent health processor cycle, which would be unacceptable.
        Set<NodeFixReport> fixReports = new HashSet<>();
        fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.PURGE));
        fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX));

        return fixReports;
    }
}
