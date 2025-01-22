package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

@Slf4j
public class SingleTransactionIndexingStrategy implements IndexingStrategy {

    private final static @NonNull HashSet<@NonNull Runnable> startListeners = new HashSet<>(1);
    private final static @NonNull HashSet<@NonNull Runnable> stopListeners = new HashSet<>(1);

    private final @NonNull TrackingComponent trackingComponent;
    private final @NonNull SingleTransactionIndexingConfiguration configuration;
    private final @NonNull AtomicReference<@NonNull CycleProgress> cycleProgress = new AtomicReference<>(NullCycleProgress.getInstance());
    private final @NonNull SingleTransactionIndexingState state = new SingleTransactionIndexingState();
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

        this.trackingComponent = trackingComponent;
        this.configuration = configuration;
    }

    @Override
    @Synchronized("state")
    public void onStart() {
        log.debug("SingleTransactionIndexingStrategy has been started.");
        state.setCurrentTxnId(configuration.getStartTxnId());
        if (state.getCurrentTxnId() < 0) state.setCurrentTxnId(1);
        cycleProgress.set(new SimpleCycleProgress(configuration.getStartTxnId(), configuration.getStopTxnId(), progressSupplier));

        announceIndexerStart();
    }


    @Override
    @Synchronized("state")
    public @NonNull Set<NodeRef> getNextNodeIds(int ignored) {
        long currentTxnId = state.getCurrentTxnId();
        log.debug("Currently processing transaction with ID ({}).", currentTxnId);
        if (currentTxnId > configuration.getStopTxnId() || currentTxnId > trackingComponent.getMaxTxnId()) return Set.of();

        state.setCurrentTxnId(currentTxnId + 1);
        return trackingComponent.getNodesForTxnIds(List.of(currentTxnId))
                .stream()
                .map(TrackingComponent.NodeInfo::getNodeRef)
                .collect(Collectors.toSet());
    }

    @Override
    @Synchronized("state")
    public void onStop() {
        log.debug("SingleTransactionIndexingStrategy has been stopped.");
        state.setCurrentTxnId(configuration.getStartTxnId());
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

}
