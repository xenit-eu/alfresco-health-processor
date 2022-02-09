package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.plugins.solr.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.plugins.solr.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SolrMissingNodeFixerPlugin extends AbstractSolrNodeFixerPlugin {
    private Map<SearchEndpointTxId, NodeFixReport> searchEndpointTxCache = new HashMap<>();

    public SolrMissingNodeFixerPlugin(SolrRequestExecutor solrRequestExecutor) {
        super(solrRequestExecutor);
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
            NodeIndexHealthReport endpointHealthReport) {
        if (endpointHealthReport.getHealthStatus() != IndexHealthStatus.NOT_FOUND) {
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
                SolrNodeCommand.REINDEX);

        searchEndpointTxCache.put(searchEndpointTxId, nodeFixReport);
        return Collections.singleton(nodeFixReport);
    }

    @Value
    public static class SearchEndpointTxId {
        private final SearchEndpoint searchEndpoint;
        private final Long txId;
    }

    private void clearCache() {
        searchEndpointTxCache.clear();
    }

}
