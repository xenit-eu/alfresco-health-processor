package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
@AllArgsConstructor
public class SolrUndersizedTransactionsFixerPluginImpl implements SolrUndersizedTransactionsFixerPlugin {

    private final static @NonNull String MERGED_MESSAGE = "The description of the node has been modified to trigger " +
            "Alfresco to merge the transaction this node was part from with another one.";
    private final static QName CM_DESCRIPTION = QName.createQName("{http://www.alfresco.org/model/content/1.0}description");

    private boolean enabled;
    private final @NonNull TransactionHelper transactionHelper;
    private final @NonNull NodeService nodeService;

    @Nonnull
    @Override
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> unhealthyReports) {
        log.debug("Received {} unhealthy nodes.", unhealthyReports.size());

        transactionHelper.inNewTransaction(() -> unhealthyReports.stream()
                .map(NodeHealthReport::getNodeRef)
                .forEach(nodeRef -> nodeService.setProperty(nodeRef, CM_DESCRIPTION, MERGED_MESSAGE)),false);

        return NodeFixReport.ofFixed(unhealthyReports, MERGED_MESSAGE);
    }



}
