package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.plugins.api.ToggleableHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SolrUndersizedTransactionsHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final static @NonNull QName ASPECT_QNAME = QName.createQName("http://www.alfresco.org/model/healthprocessor/1.0", "mergeAspect");

    private final @NonNull TransactionHelper transactionHelper;
    private final @NonNull AtomicInteger queuedMergeRequests = new AtomicInteger(0);
    private final @NonNull ExecutorService mergerExecutor;
    private final @NonNull NodeService nodeService;
    private final @NonNull BehaviourFilter behaviourFilter;

    public SolrUndersizedTransactionsHealthProcessorPlugin(boolean enabled, int mergerThreads,
                                                           @NonNull TransactionHelper transactionHelper,
                                                           @NonNull NodeService nodeService,
                                                           @NonNull BehaviourFilter behaviourFilter) {
        super(enabled);
//        guaranteeSingleTransactionIndexerIsUsed(properties); TODO.

        this.transactionHelper = transactionHelper;
        this.mergerExecutor = Executors.newFixedThreadPool(mergerThreads);
        this.nodeService = nodeService;
        this.behaviourFilter = behaviourFilter;
    }

    @Nonnull
    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> allNodeRefs) {
        // This is NOT just a logging statement; the counter is incremented! Do not remove!
        log.debug("Queueing a new batch of ({}) transaction(s). Currently, there are ({}) merge requests queued.",
                allNodeRefs.size(), queuedMergeRequests.incrementAndGet());
        mergerExecutor.submit(() -> mergeTransactions(allNodeRefs));
        return NodeHealthReport.ofHealthy(allNodeRefs);
    }

    private void mergeTransactions(@NonNull Set<@NonNull NodeRef> backgroundWorkerBatch) {
        try {
            log.debug("Merging a new batch of ({}) node(s).", backgroundWorkerBatch.size());
            AuthenticationUtil.runAsSystem(() -> {
                transactionHelper.inNewTransaction(() -> {
                    behaviourFilter.disableBehaviour();
                    for (NodeRef nodeRef : backgroundWorkerBatch) {
                        nodeService.addAspect(nodeRef, ASPECT_QNAME, Map.of());
                        nodeService.removeAspect(nodeRef, ASPECT_QNAME);
                    }
                }, false);

                return null;
            });
        } catch (Exception e) {
            log.error("An error occurred while merging a batch of ({}) node(s).", backgroundWorkerBatch.size(), e);
        } finally {
            queuedMergeRequests.decrementAndGet();
        }
    }

    // TODO: implement getConfiguration.

    @Override
    public Map<String, String> getState() {
        // TODO: update this.
        HashMap<String, String> returnValue = new HashMap<>(super.getState());
        returnValue.put("queued-merge-requests", String.valueOf(queuedMergeRequests.get()));
        return returnValue;
    }
}