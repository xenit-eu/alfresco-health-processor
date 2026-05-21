package eu.xenit.alfresco.healthprocessor.fixer.solr;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutorImpl;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
public class SolrDuplicateNodeFixerPluginImpl extends AbstractSolrNodeFixerPlugin implements SolrDuplicateNodeFixerPlugin {

    public SolrDuplicateNodeFixerPluginImpl(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, SolrRequestExecutorImpl solrRequestExecutor) {
        super(searchApplicationContextFactory, subsystemName, solrRequestExecutor);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<SolrEndpoint> endpointHealthReport) {
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.DUPLICATE
                && endpointHealthReport.getHealthStatus() != IndexHealthStatus.FOUND_UNDELETED) {
            return Collections.emptySet();
        }
        // When a duplicate node is detected, purge it from the index and reindex it
        // According to MetadataTracker#maintenance(), purge is processed before reindex
        // Even if it is executed in the same maintenance cycle.
        // Ref: https://github.com/Alfresco/SearchServices/blob/e7f05e2f13a709cd28afa3ae6acfd3d0000b22ff/search-services/alfresco-search/src/main/java/org/alfresco/solr/tracker/MetadataTracker.java#L257-L266
        // Purge has to be done before reindex, else we end up with a broken index which will only be fixed
        // by a subsequent health processor cycle, which would be unacceptable.
        Set<NodeFixReport> fixReports = new HashSet<>();
        NodeFixReport purgeStatus = trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.PURGE);
        fixReports.add(purgeStatus);
        if(purgeStatus.getFixStatus() == NodeFixStatus.SUCCEEDED) {
            fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX));
        }

        return fixReports;
    }

}
