package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.EndpointHealthReport.EndpointHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrIndexValidationHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrSearchExecutor;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                if (endpointHealthReport.getHealthStatus() == EndpointHealthStatus.NOT_FOUND) {
                    try {
                        log.debug("Requesting index for node {} on {}",
                                endpointHealthReport.getNodeRefStatus().getNodeRef(),
                                endpointHealthReport.getEndpoint());
                        boolean isReIndexed = solrSearchExecutor.forceNodeIndex(endpointHealthReport.getEndpoint(),
                                endpointHealthReport.getNodeRefStatus());
                        if (isReIndexed) {
                            fixReports.add(new NodeFixReport(NodeFixStatus.SUCCEEDED, unhealthyReport,
                                    "Reindex scheduled on " + endpointHealthReport.getEndpoint()));
                        } else {
                            fixReports.add(new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                                    "Reindex failed to schedule on " + endpointHealthReport.getEndpoint()));
                        }

                    } catch (Exception e) {
                        log.error("Error when indexing node {} on {}",
                                endpointHealthReport.getNodeRefStatus().getNodeRef(),
                                endpointHealthReport.getEndpoint(), e);
                        fixReports.add(new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                                "Exception when reindexing in " + endpointHealthReport.getEndpoint()));
                    }
                }
            }
        }

        return fixReports;
    }
}
