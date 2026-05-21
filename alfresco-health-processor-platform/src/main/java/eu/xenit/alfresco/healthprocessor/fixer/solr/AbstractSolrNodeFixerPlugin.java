package eu.xenit.alfresco.healthprocessor.fixer.solr;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrActionResponse;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.fixer.SubsystemDependantHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper=true)
@Slf4j
abstract class AbstractSolrNodeFixerPlugin extends SubsystemDependantHealthFixerPlugin<SolrEndpoint> {

    @EqualsAndHashCode.Exclude
    private final SolrRequestExecutor solrRequestExecutor;

    public AbstractSolrNodeFixerPlugin(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, SolrRequestExecutor solrRequestExecutor)
    {
        super(SolrEndpoint.class, searchApplicationContextFactory, subsystemName);
        this.solrRequestExecutor = solrRequestExecutor;
    }

    protected NodeFixReport trySendSolrCommand(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<SolrEndpoint> endpointHealthReport, SolrNodeCommand command) {
        try {
            log.debug("Requesting {} for node {} on {}",
                    command,
                    endpointHealthReport.getNodeRefStatus().getNodeRef(),
                    endpointHealthReport.getEndpoint());
            SolrActionResponse solrActionResponse = solrRequestExecutor.executeAsyncNodeCommand((SolrEndpoint)endpointHealthReport.getEndpoint(),
                            endpointHealthReport.getNodeRefStatus(), command);
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
