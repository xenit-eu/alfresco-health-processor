package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public class ThresholdIndexingStrategy implements IndexingStrategy {

    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;
    private final @NonNull AbstractNodeDAOImpl nodeDAO;
    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull ThresholdIndexingStrategyState state = new ThresholdIndexingStrategyState();
    private final @NonNull ThresholdIndexingStrategyTransactionIdFetcher transactionIdFetcher;
    private final @NonNull BlockingDeque<Set<NodeRef>> queuedNodes;
    private final @NonNull ThresholdIndexingStrategyTransactionIdMerger @NonNull [] transactionIdMergers;
    private final @NonNull HashSet<Thread> runningThreads;
    private final @NonNull AtomicReference<CycleProgress> cycleProgress = new AtomicReference<>(NullCycleProgress.getInstance());
    private final @NonNull LongSupplier progressReporter = state::getCurrentTransactionId;

    public ThresholdIndexingStrategy(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                     @NonNull AbstractNodeDAOImpl nodeDAO,
                                     @NonNull SearchTrackingComponent searchTrackingComponent) {
        this.configuration = configuration;
        this.searchTrackingComponent = searchTrackingComponent;
        this.nodeDAO = nodeDAO;

        this.runningThreads = new HashSet<>(configuration.getTransactionsBackgroundWorkers() + 1);
        this.transactionIdFetcher = new ThresholdIndexingStrategyTransactionIdFetcher(configuration, searchTrackingComponent, state);
        this.queuedNodes = new LinkedBlockingDeque<>(configuration.getTransactionsBackgroundWorkers());

        this.transactionIdMergers = new ThresholdIndexingStrategyTransactionIdMerger[configuration.getTransactionsBackgroundWorkers()];
        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i++)
            this.transactionIdMergers[i] = new ThresholdIndexingStrategyTransactionIdMerger(transactionIdFetcher, queuedNodes, configuration, searchTrackingComponent, state);
    }

    @Override
    public void onStart() {
        state.setCurrentTransactionId(Math.max(configuration.getMinTransactionId(), nodeDAO.getMinTxnId()));
        state.setMaxTransactionId(Math.min(configuration.getMaxTransactionId() >= 0? configuration.getMaxTransactionId() : Long.MAX_VALUE, searchTrackingComponent.getMaxTxnId()));

        cycleProgress.set(new SimpleCycleProgress(state.getCurrentTransactionId(), state.getMaxTransactionId(), progressReporter));

        runningThreads.add(new Thread(transactionIdFetcher));
        for (ThresholdIndexingStrategyTransactionIdMerger merger : transactionIdMergers) runningThreads.add(new Thread(merger));
        for (Thread thread : runningThreads) thread.start();
        state.setRunningTransactionMergers(transactionIdMergers.length);
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public @NonNull Set<NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> returnValue = Set.of();
        while (state.getRunningTransactionMergers() > 0) {
            returnValue = queuedNodes.takeFirst();
            if (returnValue.isEmpty()) state.decrementRunningTransactionMergers();
            else break;
        }

        return returnValue;
    }

    @Override
    public void onStop() {
        for (Thread thread : runningThreads) thread.interrupt();
        runningThreads.clear();

        state.setCurrentTransactionId(-1);
        // I'm leaving maxTransactionId as-is. Gives a nice indicator where the previous iteration finished.

        cycleProgress.set(NullCycleProgress.getInstance());
    }

    @Override
    public @NonNull Map<String, String> getState() {
        return state.getMapRepresentation();
    }

    @Override
    public @NonNull CycleProgress getCycleProgress() {
        return cycleProgress.get();
    }
}