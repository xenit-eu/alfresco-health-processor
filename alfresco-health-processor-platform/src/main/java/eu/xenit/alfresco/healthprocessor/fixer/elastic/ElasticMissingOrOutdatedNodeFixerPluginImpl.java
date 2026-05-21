package eu.xenit.alfresco.healthprocessor.fixer.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.event2.EventGenerator;
import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport.IndexHealthStatus;
import eu.xenit.alfresco.healthprocessor.checker.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.endpoint.elastic.ElasticEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.ElasticResult;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ElasticMissingOrOutdatedNodeFixerPluginImpl extends AbstractElasticFixerPlugin
        implements ElasticMissingOrOutdatedNodeFixerPlugin
{

    private static final Set<IndexHealthStatus> SUPPORTED_STATES = EnumSet.of(IndexHealthStatus.FOUND_OUTDATED,
            IndexHealthStatus.FOUND_PATH_MISSING, IndexHealthStatus.NOT_FOUND);

    // copied from AbstractNodeDAOImpl
    private static final String KEY_TRANSACTION = "node.transaction.id";

    @EqualsAndHashCode.Exclude
    private final RetryingTransactionHelper transactionHelper;

    @EqualsAndHashCode.Exclude
    private final NodeService nodeService;

    @EqualsAndHashCode.Exclude
    private final EventGenerator eventGenerator;

    private @Setter boolean waitForIndex;

    private @Setter int waitIntervalMillis;

    private @Setter int waitIntervalCount;

    public ElasticMissingOrOutdatedNodeFixerPluginImpl(SwitchableApplicationContextFactory searchApplicationContextFactory,
            String subsystemName, ElasticRequestExecutor elasticRequestExecutor, RetryingTransactionHelper transactionHelper,
            NodeService nodeService, EventGenerator eventGenerator)
    {
        super(searchApplicationContextFactory, subsystemName, elasticRequestExecutor);
        this.transactionHelper = transactionHelper;
        this.nodeService = nodeService;
        this.eventGenerator = eventGenerator;
    }

    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> unhealthyReports)
    {
        Set<NodeFixReport> fixReports = new HashSet<>();

        Map<ElasticEndpoint, List<Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>>> unhealthyNodeIndexReportsByEndpoint = new HashMap<>();
        for (NodeHealthReport unhealthyReport : unhealthyReports)
        {
            unhealthyReport.data(NodeIndexHealthReport.class).stream().filter(hr -> endpointClass.isInstance(hr.getEndpoint()))
                    .filter(hr -> SUPPORTED_STATES.contains(hr.getHealthStatus())).forEach(hr -> {
                        @SuppressWarnings("unchecked")
                        NodeIndexHealthReport<ElasticEndpoint> report = (NodeIndexHealthReport<ElasticEndpoint>) hr;
                        unhealthyNodeIndexReportsByEndpoint.computeIfAbsent(report.getEndpoint(), k -> new ArrayList<>())
                                .add(new Pair<>(unhealthyReport, report));
                    });
        }

        for (Entry<ElasticEndpoint, List<Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>>> entry : unhealthyNodeIndexReportsByEndpoint
                .entrySet())
        {
            fixReports.addAll(processReports(entry.getKey(), entry.getValue()));
        }

        return fixReports;
    }

    @Override
    protected Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<ElasticEndpoint> endpointHealthReport)
    {
        throw new UnsupportedOperationException("Should not be called");
    }

    private Set<NodeFixReport> processReports(ElasticEndpoint endpoint,
            List<Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> reports)
    {
        final boolean expectPathsIndexed = endpoint.isExpectPathsIndexed();

        if (!expectPathsIndexed)
        {
            return processReportsSimple(endpoint, reports);
        }
        else
        {
            return processReportsWithParents(endpoint, reports);
        }
    }

    private Set<NodeFixReport> processReportsSimple(ElasticEndpoint endpoint,
            List<Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> reports)
    {
        Set<NodeFixReport> fixReports = new HashSet<>();
        Map<Status, Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> hrByStatus = new HashMap<>();
        Collection<Status> toIndex = new HashSet<>();
        for (Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>> report : reports)
        {
            Status nodeRefStatus = report.getSecond().getNodeRefStatus();
            hrByStatus.put(nodeRefStatus, report);
            toIndex.add(nodeRefStatus);
        }

        Collection<Status> indexed = doIndex(endpoint, toIndex);
        for (Status indexedStatus : indexed)
        {
            fixReports.add(new NodeFixReport(NodeFixStatus.SUCCEEDED, hrByStatus.remove(indexedStatus).getFirst(), "Reindexed (verified)"));
        }
        for (Entry<Status, Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> entry : hrByStatus.entrySet())
        {
            fixReports.add(new NodeFixReport(NodeFixStatus.SUCCEEDED, entry.getValue().getFirst(), "Reindex event sent (not yet verified)"));
        }
        return fixReports;
    }

    private Set<NodeFixReport> processReportsWithParents(ElasticEndpoint endpoint,
            List<Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> reports)
    {
        // using NodeRef for key/identity instead of Status - latter has no usable equals/hashCode
        Map<NodeRef, Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> hrByNodeRef = new HashMap<>();
        Map<NodeRef, Status> statusByNodeRef = new HashMap<>();
        Collection<NodeRef> toIndex = new HashSet<>();

        for (Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>> report : reports)
        {
            Status s = report.getSecond().getNodeRefStatus();
            NodeRef n = s.getNodeRef();
            hrByNodeRef.put(n, report);
            toIndex.add(n);
            statusByNodeRef.put(n, s);
        }

        Map<NodeRef, Collection<NodeRef>> childrenByParent = new HashMap<>();
        Map<NodeRef, Collection<NodeRef>> parentsByChild = new HashMap<>();

        // 1. step: traverse hierarchies, check parents, and collect graphs of parent-child/child-parent relations for index relevant
        checkAndBuildIndexableHierarchy(endpoint, hrByNodeRef, statusByNodeRef, toIndex, childrenByParent, parentsByChild);

        // 2. step: process hierarchies
        return processIndexableHierarchy(endpoint, hrByNodeRef, statusByNodeRef, toIndex, childrenByParent, parentsByChild);
    }

    private void checkAndBuildIndexableHierarchy(ElasticEndpoint endpoint,
            Map<NodeRef, Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> hrByNodeRef, Map<NodeRef, Status> statusByNodeRef,
            Collection<NodeRef> toIndex, Map<NodeRef, Collection<NodeRef>> childrenByParent,
            Map<NodeRef, Collection<NodeRef>> parentsByChild)
    {
        Collection<NodeRef> toProcess = new HashSet<>(toIndex);
        Collection<NodeRef> checkedParents = new HashSet<>();

        Consumer<Status> irrelevantParentStatusConsumer = s -> {
            childrenByParent.get(s.getNodeRef()).stream().forEach(c -> parentsByChild.get(c).remove(s.getNodeRef()));
        };
        BiConsumer<Status, IndexHealthStatus> relevantParentStatusConsumer = (s, hs) -> {
            toProcess.add(s.getNodeRef());
            toIndex.add(s.getNodeRef());
            NodeIndexHealthReport<ElasticEndpoint> ihr = new NodeIndexHealthReport<ElasticEndpoint>(hs, s, endpoint);
            NodeHealthReport nhr = new NodeHealthReport(NodeHealthStatus.UNHEALTHY, s.getNodeRef(), ihr.getMessage());
            hrByNodeRef.put(s.getNodeRef(), new Pair<>(nhr, ihr));
        };

        while (!toProcess.isEmpty())
        {
            log.debug("Trying to find index-requiring parents from node statuses: {}", toProcess);

            Map<NodeRef, Status> parents = new HashMap<>();
            toProcess.stream().forEach(n -> {
                Status s = statusByNodeRef.computeIfAbsent(n, nodeService::getNodeStatus);
                statusByNodeRef.put(n, s);
                // we are NOT filtering by node event filters here
                // e.g. a sys:store_root should not be filtered despite the event nodeType filter including sys:*
                // PATH indexing of any node depends on an unbroken chain of paths on any node along the hierarchy
                nodeService.getParentAssocs(n).stream().map(ChildAssociationRef::getParentRef).forEach(p -> {
                    childrenByParent.computeIfAbsent(p, k -> new HashSet<>()).add(n);
                    parentsByChild.computeIfAbsent(n, k -> new HashSet<>()).add(p);
                    if (!checkedParents.contains(p) && !toIndex.contains(p) && !parents.containsKey(p))
                    {
                        Status parentStatus = statusByNodeRef.computeIfAbsent(p, nodeService::getNodeStatus);
                        parents.put(p, parentStatus);
                    }
                });
            });

            toProcess.clear();

            if (!parents.isEmpty())
            {
                log.debug("Checking parent node statuses: {}", parents);

                ElasticResult checkResult;
                try
                {
                    checkResult = elasticRequestExecutor.checkNodeIndexed(endpoint, parents.values());
                }
                catch (IOException e)
                {
                    throw new AlfrescoRuntimeException("Error performing parent index state check for implicit indexing requirements", e);
                }

                checkedParents.addAll(parents.keySet());
                checkResult.getFound().forEach(irrelevantParentStatusConsumer);
                checkResult.getSuperfluous().forEach(irrelevantParentStatusConsumer);
                checkResult.getMissing().forEach(s -> relevantParentStatusConsumer.accept(s, IndexHealthStatus.NOT_FOUND));
                checkResult.getOutdated().forEach(s -> relevantParentStatusConsumer.accept(s, IndexHealthStatus.FOUND_OUTDATED));
                checkResult.getPathMissing().forEach(s -> relevantParentStatusConsumer.accept(s, IndexHealthStatus.FOUND_PATH_MISSING));
            }
            else
            {
                log.debug("No more parents to check");
            }
        }
    }

    private Set<NodeFixReport> processIndexableHierarchy(ElasticEndpoint endpoint,
            Map<NodeRef, Pair<NodeHealthReport, NodeIndexHealthReport<ElasticEndpoint>>> hrByNodeRef, Map<NodeRef, Status> statusByNodeRef,
            Collection<NodeRef> toIndex, Map<NodeRef, Collection<NodeRef>> childrenByParent,
            Map<NodeRef, Collection<NodeRef>> parentsByChild)
    {
        Set<NodeFixReport> fixReports = new HashSet<>();
        while (!toIndex.isEmpty())
        {
            Collection<Status> toIndexIteration = new HashSet<>();
            for (NodeRef n : toIndex)
            {
                // no (more) dependencies
                if (!parentsByChild.containsKey(n) || parentsByChild.get(n).isEmpty())
                {
                    toIndexIteration.add(statusByNodeRef.get(n));
                }
            }

            Collection<Status> verifiedFixed = doIndex(endpoint, toIndexIteration);
            for (Status status : toIndexIteration)
            {
                NodeRef n = status.getNodeRef();
                // verifiedFixed.contains only works because result of doIndex is guaranteed to use input instances
                // so contains works on object identity here (Status has no equals/hashCode)
                fixReports.add(new NodeFixReport(NodeFixStatus.SUCCEEDED, hrByNodeRef.remove(n).getFirst(),
                        verifiedFixed.contains(status) ? "Reindexed (verified)" : "Reindex event sent (not yet verified)"));
                if (childrenByParent.containsKey(n))
                {
                    childrenByParent.get(n).stream().forEach(c -> parentsByChild.get(c).remove(n));
                }
                childrenByParent.remove(n);
                parentsByChild.remove(n);
                toIndex.remove(n);
            }
        }

        return fixReports;
    }

    private Collection<Status> doIndex(ElasticEndpoint endpoint, Collection<Status> nodeStatuses)
    {
        Set<Status> fixed = new HashSet<>();

        log.debug("Trying to trigger index for statuses: {}", nodeStatuses);

        // need a read-write txn in case of outbox sender
        transactionHelper.doInTransaction(() -> {
            // need a fake txn entity which EventGenerator checks or it won't send events
            TransactionEntity txn = new TransactionEntity();
            txn.setCommitTimeMs(System.currentTimeMillis());
            AlfrescoTransactionSupport.bindResource(KEY_TRANSACTION, txn);

            for (Status status : nodeStatuses)
            {
                NodeRef nodeRef = status.getNodeRef();
                log.trace("Triggering 'fake' creation event to have live indexer app index missing node {}", nodeRef);
                eventGenerator.onCreateNode(nodeService.getPrimaryParent(nodeRef));
            }

            return null;
        }, false, true);

        if (waitForIndex)
        {
            runWaitAndCheckLoop(endpoint, nodeStatuses, fixed);
        }

        return fixed;
    }

    private void runWaitAndCheckLoop(ElasticEndpoint endpoint, Collection<Status> nodeStatuses, Set<Status> fixed)
    {
        Collection<Status> toCheck = new HashSet<>(nodeStatuses);
        int waitCount = 0;
        while (waitCount++ < waitIntervalCount && !toCheck.isEmpty())
        {
            log.debug("Running wait + check loop #{} for statuses to check: {}", waitCount, toCheck);
            try
            {
                Thread.sleep(waitIntervalMillis);
            }
            catch (InterruptedException e)
            {
                log.warn("Thread interrupted while sleeping before next index check");
                Thread.currentThread().interrupt();
                break;
            }

            ElasticResult checkResult;
            try
            {
                checkResult = elasticRequestExecutor.checkNodeIndexed(endpoint, toCheck);
            }
            catch (IOException e)
            {
                log.warn("Error checking node index states after index trigger", e);
                break;
            }

            Set<Status> found = checkResult.getFound();
            if (!found.isEmpty())
            {
                log.debug("Found fixed index entries in loop #{} for statuses: {}", waitCount, found);
            }
            fixed.addAll(found);
            toCheck.removeAll(found);
        }
    }
}
