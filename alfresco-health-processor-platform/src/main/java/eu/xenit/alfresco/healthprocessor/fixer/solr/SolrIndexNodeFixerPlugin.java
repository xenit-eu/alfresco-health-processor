package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport.EndpointHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrIndexValidationHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;

@AllArgsConstructor
@Slf4j
public class SolrIndexNodeFixerPlugin extends ToggleableHealthFixerPlugin {

    private final SolrSearchExecutor solrSearchExecutor;

    @Nonnull
    @Override
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass,
            Set<NodeHealthReport> unhealthyReports) {
        if (pluginClass != SolrIndexValidationHealthProcessorPlugin.class) {
            return unhealthyReports.stream()
                    .map(report -> new NodeFixReport(NodeFixStatus.SKIPPED, report))
                    .collect(Collectors.toSet());
        }

        Set<NodeFixReport> fixReports = new HashSet<>();

        for (NodeHealthReport unhealthyReport : unhealthyReports) {
            Set<EndpointHealthReport> endpointHealthReports = unhealthyReport.data(EndpointHealthReport.class);

            for (EndpointHealthReport endpointHealthReport : endpointHealthReports) {
                switch (endpointHealthReport.getHealthStatus()) {
                    case NOT_FOUND:
                        fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX));
                        break;
                    case DUPLICATE:
                        // When a duplicate node is detected, purge it from the index and reindex it
                        // According to MetadataTracker#maintenance(), purge is processed before reindex
                        // Even if it is executed in the same maintenance cycle.
                        // Ref: https://github.com/Alfresco/SearchServices/blob/e7f05e2f13a709cd28afa3ae6acfd3d0000b22ff/search-services/alfresco-search/src/main/java/org/alfresco/solr/tracker/MetadataTracker.java#L257-L266
                        // Purge has to be done before reindex, else we end up with a broken index which will only be fixed
                        // by a subsequent health processor cycle, which would be unacceptable.
                        fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.PURGE));
                        fixReports.add(trySendSolrCommand(unhealthyReport, endpointHealthReport, SolrNodeCommand.REINDEX));
                        break;
                }
            }
        }

        return fixReports;
    }

    private NodeFixReport trySendSolrCommand(NodeHealthReport unhealthyReport, EndpointHealthReport endpointHealthReport, SolrNodeCommand command) {
        try {
            log.debug("Requesting index for node {} on {}",
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint());
            boolean isReIndexed = solrSearchExecutor.executeNodeCommand(endpointHealthReport.getEndpoint(),
                    endpointHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX);
            if (isReIndexed) {
                return new NodeFixReport(NodeFixStatus.SUCCEEDED, unhealthyReport,
                        command+" scheduled on " + endpointHealthReport.getEndpoint());
            } else {
                return new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                        command+" failed to schedule on " + endpointHealthReport.getEndpoint());
            }

        } catch (Exception e) {
            log.error("Error when requesting {} for node {} on {}",
                    command,
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint(), e);
            return new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                    "Exception when requesting "+ command+" on " + endpointHealthReport.getEndpoint());
        }
    }
}
