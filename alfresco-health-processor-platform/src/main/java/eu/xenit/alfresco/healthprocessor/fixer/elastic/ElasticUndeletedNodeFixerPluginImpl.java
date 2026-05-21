package eu.xenit.alfresco.healthprocessor.fixer.elastic;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.endpoint.elastic.ElasticEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor.ActionResponse;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ElasticUndeletedNodeFixerPluginImpl extends AbstractElasticFixerPlugin implements ElasticUndeletedNodeFixerPlugin
{

    public ElasticUndeletedNodeFixerPluginImpl(SwitchableApplicationContextFactory searchApplicationContextFactory, String subsystemName,
            ElasticRequestExecutor elasticRequestExecutor)
    {
        super(searchApplicationContextFactory, subsystemName, elasticRequestExecutor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<ElasticEndpoint> endpointHealthReport)
    {
        // duplicates are not possible in Elastic
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.FOUND_UNDELETED)
        {
            return Collections.emptySet();
        }

        NodeFixReport fixReport;

        try
        {
            ActionResponse response = elasticRequestExecutor.deleteNodeIndexEntry(endpointHealthReport.getEndpoint(),
                    unhealthyReport.getNodeRef().getId());

            if (response.isSuccessFull())
            {
                fixReport = new NodeFixReport(NodeFixStatus.SUCCEEDED, unhealthyReport,
                        "Node index entry deleted on " + endpointHealthReport.getEndpoint());
            }
            else
            {
                fixReport = new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                        "Failed to delete node index entry on " + endpointHealthReport.getEndpoint());
            }
        }
        catch (IOException ioex)
        {
            fixReport = new NodeFixReport(NodeFixStatus.FAILED, unhealthyReport,
                    "Failed to delete node index entry on " + endpointHealthReport.getEndpoint() + " : " + ioex.getMessage());
        }

        return Collections.singleton(fixReport);
    }

}
