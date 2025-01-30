package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections4.set.UnmodifiableSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

@Slf4j
public class SingleTransactionIndexingStrategy implements IndexingStrategy {

    public final static @NonNull String selectedIndexingStrategyPropertyKey = "eu.xenit.alfresco.healthprocessor.indexing.strategy";
    public final static @NonNull IndexingStrategyKey indexingStrategyKey = IndexingStrategyKey.SINGLE_TXNS;

    private final static @NonNull HashSet<@NonNull Runnable> startListeners = new HashSet<>(1);
    private final static @NonNull HashSet<@NonNull Runnable> stopListeners = new HashSet<>(1);

    private final @NonNull SingleTransactionIndexingConfiguration configuration;
    private final @NonNull TrackingComponent trackingComponent;
    private final @NonNull AtomicReference<@NonNull CycleProgress> cycleProgress = new AtomicReference<>(NullCycleProgress.getInstance());
    private final @NonNull SingleTransactionIndexingState state = new SingleTransactionIndexingState();
    private final @NonNull SingleTransactionIndexingBackgroundWorker backgroundWorker;
    private final @NonNull LongSupplier progressSupplier = () -> {
        synchronized (state) {
            return state.getCurrentTxnId();
        }
    };

    public SingleTransactionIndexingStrategy(@NonNull TrackingComponent trackingComponent,
                                             @NonNull SingleTransactionIndexingConfiguration configuration) {
        log.warn("A SingleTransactionIndexingStrategy has been created as part of the health processor setup. " +
                "Please note that this strategy ignores the amount of requested nodeRefs and always returns exactly " +
                "one transaction worth of nodeRefs.");

        this.backgroundWorker = new SingleTransactionIndexingBackgroundWorker(trackingComponent, configuration, state);
        this.configuration = configuration;
        this.trackingComponent = trackingComponent;

        this.state.setCurrentTxnId(-1);
        this.state.setLastTxnId(configuration.getStopTxnId());
    }

    @Override
    @Synchronized("state")
    public void onStart() {
        log.debug("SingleTransactionIndexingStrategy has been started.");

        state.setCurrentTxnId(configuration.getStartTxnId());
        if (state.getCurrentTxnId() < 0) state.setCurrentTxnId(1);
        state.setLastTxnId(Math.min(configuration.getStopTxnId(), trackingComponent.getMaxTxnId()));
        cycleProgress.set(new SimpleCycleProgress(state.getCurrentTxnId(), state.getLastTxnId(), progressSupplier));

        Thread backgroundWorkerThread = new Thread(backgroundWorker);
        backgroundWorkerThread.setName("SingleTransactionIndexingBackgroundWorker");
        backgroundWorkerThread.start();

        announceIndexerStart();
    }


    @Override
    @Synchronized("state")
    @SneakyThrows(InterruptedException.class) // Not possible with the current code.
    public @NonNull Set<NodeRef> getNextNodeIds(int ignored) {
        Set<NodeRef> returnValue = backgroundWorker.takeNextTransaction();
        state.setCurrentTxnId(state.getCurrentTxnId() + 1);
        return returnValue;
    }

    @Override
    @Synchronized("state")
    public void onStop() {
        log.debug("SingleTransactionIndexingStrategy has been stopped.");
        state.setCurrentTxnId(-1);
        cycleProgress.set(NullCycleProgress.getInstance());

        announceIndexerStop();
    }

    @Override
    @Synchronized("state")
    public @NonNull Map<String, String> getState() {
        return state.generateMapRepresentation();
    }

    @Override
    public @NonNull CycleProgress getCycleProgress() {
        return cycleProgress.get();
    }

    @Synchronized("startListeners")
    public static void listenToIndexerStart(@NonNull Runnable runnable) {
        startListeners.add(runnable);
    }

    @Synchronized("startListeners")
    private static void announceIndexerStart() {
        startListeners.forEach(Runnable::run);
    }

    @Synchronized("stopListeners")
    public static void listenToIndexerStop(@NonNull Runnable runnable) {
        stopListeners.add(runnable);
    }

    @Synchronized("stopListeners")
    private static void announceIndexerStop() {
        stopListeners.forEach(Runnable::run);
    }

    public static boolean isSelectedIndexingStrategy(@NonNull Properties properties) {
        return indexingStrategyKey.getKey().equals(properties.get(selectedIndexingStrategyPropertyKey));
    }

}
