package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrIndexValidationHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
abstract class AbstractSolrNodeFixerPlugin extends ToggleableHealthFixerPlugin {

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
                fixReports.addAll(handleHealthReport(unhealthyReport, endpointHealthReport));
            }
        }

        return fixReports;
    }

    protected abstract Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            EndpointHealthReport endpointHealthReport);

    protected NodeFixReport trySendSolrCommand(NodeHealthReport unhealthyReport,
            EndpointHealthReport endpointHealthReport, SolrNodeCommand command) {
        try {
            log.debug("Requesting index for node {} on {}",
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint());
            boolean isReIndexed = solrSearchExecutor.executeNodeCommand(endpointHealthReport.getEndpoint(),
                    endpointHealthReport.getNodeRefStatus(), SolrNodeCommand.REINDEX);
            if (isReIndexed) {
                return new NodeFixReport(NodeFixStatus.SUCCEEDED, unhealthyReport,
                        command + " scheduled on " + endpointHealthReport.getEndpoint());
            } else {
                return new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                        command + " failed to schedule on " + endpointHealthReport.getEndpoint());
            }

        } catch (Exception e) {
            log.error("Error when requesting {} for node {} on {}",
                    command,
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint(), e);
            return new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                    "Exception when requesting " + command + " on " + endpointHealthReport.getEndpoint());
        }
    }
}
