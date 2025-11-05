package eu.xenit.alfresco.healthprocessor.fixer.solr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.alfresco.service.cmr.repository.NodeRef;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.checker.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutorImpl;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(callSuper=true)
public class SolrMissingNodeFixerPluginImpl extends AbstractSolrNodeFixerPlugin implements SolrMissingNodeFixerPlugin {

    private Map<SearchEndpointTxId, NodeFixReport> searchEndpointTxCache = new HashMap<>();

    public SolrMissingNodeFixerPluginImpl(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, SolrRequestExecutorImpl solrRequestExecutor) {
        super(searchApplicationContextFactory, subsystemName, solrRequestExecutor);
    }

    @Nonnull
    @Override
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass,
                                  Set<NodeHealthReport> unhealthyReports) {
        clearCache();
        return super.fix(pluginClass, unhealthyReports);
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<SolrEndpoint> endpointHealthReport) {
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.NOT_FOUND
                && endpointHealthReport.getHealthStatus() != IndexHealthStatus.FOUND_OUTDATED) {
            return Collections.emptySet();
        }

        NodeRef.Status nodeStatus = endpointHealthReport.getNodeRefStatus();
        SearchEndpointTxId searchEndpointTxId = new SearchEndpointTxId(endpointHealthReport.getEndpoint(), nodeStatus.getDbTxnId());
        if (searchEndpointTxCache.containsKey(searchEndpointTxId)) {
            NodeFixReport cachedNodeFixReport = searchEndpointTxCache.get(searchEndpointTxId);
            log.trace("We already have a fix report for {}: {}", searchEndpointTxId, cachedNodeFixReport);
            //If a successful reindex action was already sent for this tx to this endpoint, do not schedule another one
            if (cachedNodeFixReport.getFixStatus() == NodeFixStatus.SUCCEEDED) {
                log.trace("Fix for TX of {} has already succeeded, sending existing fix report messages.", unhealthyReport);
                return Collections.singleton(new NodeFixReport(cachedNodeFixReport.getFixStatus(), unhealthyReport,
                        cachedNodeFixReport.getMessages()));
            }
        }

        log.trace("Performing reindex for {}", searchEndpointTxId);

        // Action not yet (successfully) sent
        NodeFixReport nodeFixReport = trySendSolrCommand(unhealthyReport, endpointHealthReport,
                SolrNodeCommand.REINDEX_TRANSACTION);

        searchEndpointTxCache.put(searchEndpointTxId, nodeFixReport);
        return Collections.singleton(nodeFixReport);
    }

    @Value
    public static class SearchEndpointTxId {
        private final SolrEndpoint searchEndpoint;
        private final Long txId;
    }

    private void clearCache() {
        searchEndpointTxCache.clear();
    }

}
