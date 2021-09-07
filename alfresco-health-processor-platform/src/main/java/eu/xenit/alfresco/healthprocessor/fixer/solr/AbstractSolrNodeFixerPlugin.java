package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrIndexValidationHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
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

    private final SolrRequestExecutor solrRequestExecutor;

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
            Set<NodeIndexHealthReport> endpointHealthReports = unhealthyReport.data(NodeIndexHealthReport.class);

            for (NodeIndexHealthReport endpointHealthReport : endpointHealthReports) {
                fixReports.addAll(handleHealthReport(unhealthyReport, endpointHealthReport));
            }
        }

        return fixReports;
    }

    protected abstract Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport endpointHealthReport);

    protected NodeFixReport trySendSolrCommand(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport endpointHealthReport, SolrNodeCommand command) {
        try {
            log.debug("Requesting {} for node {} on {}",
                    command,
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint());
            boolean isSuccessful = solrRequestExecutor.executeNodeCommand(endpointHealthReport.getEndpoint(),
                    endpointHealthReport.getNodeRefStatus(), command);
            if (isSuccessful) {
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
