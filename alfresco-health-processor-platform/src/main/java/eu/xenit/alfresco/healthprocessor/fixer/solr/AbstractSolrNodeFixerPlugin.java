package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrActionResponse;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
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
        Set<NodeFixReport> fixReports = new HashSet<>();
        clearCache();
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

    protected abstract void clearCache();

    protected NodeFixReport trySendSolrCommand(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport endpointHealthReport, SolrNodeCommand command, boolean targetTransaction) {
        try {
            log.debug("Requesting {} for node {} on {}",
                    command,
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint());
            SolrActionResponse solrActionResponse = solrRequestExecutor.executeAsyncNodeCommand(endpointHealthReport.getEndpoint(),
                            endpointHealthReport.getNodeRefStatus(), command, targetTransaction);
            if (solrActionResponse.isSuccessFull()) {
                return new NodeFixReport(NodeFixStatus.SUCCEEDED, unhealthyReport, command + " on " +
                        endpointHealthReport.getEndpoint() + " : " + solrActionResponse.getMessage());
            } else {
                return new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport, command + " failed to schedule on " +
                        endpointHealthReport.getEndpoint() + " : " + solrActionResponse.getMessage());
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
