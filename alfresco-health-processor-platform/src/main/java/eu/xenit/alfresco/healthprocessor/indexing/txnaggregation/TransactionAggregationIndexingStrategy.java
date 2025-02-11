package eu.xenit.alfresco.healthprocessor.indexing.txnaggregation;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

@Slf4j
public class TransactionAggregationIndexingStrategy implements IndexingStrategy, MeterBinder {

    private final @NonNull TransactionAggregationIndexingStrategyConfiguration configuration;
    private final @NonNull AbstractNodeDAOImpl nodeDAO;
    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull TransactionAggregationIndexingStrategyState state = new TransactionAggregationIndexingStrategyState();
    private final @NonNull TransactionAggregationIndexingStrategyTransactionIdFetcher transactionIdFetcher;
    private final @NonNull BlockingDeque<@NonNull Set<@NonNull NodeRef>> queuedNodes;
    private final @NonNull TransactionAggregationIndexingStrategyTransactionIdMerger @NonNull [] transactionIdMergers;
    private final @NonNull HashSet<@NonNull Thread> runningThreads;
    private final @NonNull AtomicReference<@NonNull CycleProgress> cycleProgress = new AtomicReference<>(NullCycleProgress.getInstance());
    private final @NonNull LongSupplier progressReporter = state::getCurrentTransactionId;

    public TransactionAggregationIndexingStrategy(@NonNull TransactionAggregationIndexingStrategyConfiguration configuration,
                                                  @NonNull AbstractNodeDAOImpl nodeDAO,
                                                  @NonNull SearchTrackingComponent searchTrackingComponent,
                                                  @NonNull DataSource dataSource,
                                                  @Nullable MeterRegistry meterRegistry) {
        this(configuration, nodeDAO, searchTrackingComponent, new JdbcTemplate(dataSource), meterRegistry);
    }

    TransactionAggregationIndexingStrategy(@NonNull TransactionAggregationIndexingStrategyConfiguration configuration,
                                           @NonNull AbstractNodeDAOImpl nodeDAO,
                                           @NonNull SearchTrackingComponent searchTrackingComponent,
                                           @NonNull JdbcTemplate jdbcTemplate) {
        this(configuration, nodeDAO, searchTrackingComponent, jdbcTemplate, null);
    }

    TransactionAggregationIndexingStrategy(@NonNull TransactionAggregationIndexingStrategyConfiguration configuration,
                                           @NonNull AbstractNodeDAOImpl nodeDAO,
                                           @NonNull SearchTrackingComponent searchTrackingComponent,
                                           @NonNull JdbcTemplate jdbcTemplate,
                                           @Nullable MeterRegistry meterRegistry) {
        if (configuration.getTransactionsBackgroundWorkers() <= 0)
            throw new IllegalArgumentException(String.format("The amount of background workers must be greater than zero (%d provided).", configuration.getTransactionsBackgroundWorkers()));

        this.configuration = configuration;
        this.searchTrackingComponent = searchTrackingComponent;
        this.nodeDAO = nodeDAO;

        this.runningThreads = new HashSet<>(configuration.getTransactionsBackgroundWorkers() + 1);
        this.transactionIdFetcher = new TransactionAggregationIndexingStrategyTransactionIdFetcher(configuration, jdbcTemplate, state);
        this.queuedNodes = new LinkedBlockingDeque<>(configuration.getTransactionsBackgroundWorkers());

        this.transactionIdMergers = new TransactionAggregationIndexingStrategyTransactionIdMerger[configuration.getTransactionsBackgroundWorkers()];
        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i++)
            this.transactionIdMergers[i] = new TransactionAggregationIndexingStrategyTransactionIdMerger(transactionIdFetcher, queuedNodes, configuration, searchTrackingComponent);

        if (meterRegistry != null) bindTo(meterRegistry);
    }

    @Override
    public void onStart() {
        state.setCurrentTransactionId(Math.max(configuration.getMinTransactionId(), nodeDAO.getMinTxnId()));
        state.setMaxTransactionId(Math.min(configuration.getMaxTransactionId() >= 0? configuration.getMaxTransactionId() : Long.MAX_VALUE, searchTrackingComponent.getMaxTxnId()));
        cycleProgress.set(new SimpleCycleProgress(state.getCurrentTransactionId(), state.getMaxTransactionId(), progressReporter));
        log.debug("Starting the TransactionAggregationIndexingStrategy with currentTransactionId ({}) and maxTransactionId ({}).", state.getCurrentTransactionId(), state.getMaxTransactionId());

        Thread fetcherThread = new Thread(transactionIdFetcher);
        fetcherThread.setName("TransactionAggregationIndexingStrategyTransactionIdFetcher");
        runningThreads.add(fetcherThread);
        for (int i = 0; i < transactionIdMergers.length; i++) {
            TransactionAggregationIndexingStrategyTransactionIdMerger merger = transactionIdMergers[i];
            Thread mergerThread = new Thread(merger);
            mergerThread.setName(String.format("TransactionAggregationIndexingStrategyTransactionIdMerger-%d", i));
            runningThreads.add(new Thread(merger));
        }
        for (Thread thread : runningThreads) thread.start();
        state.setRunningTransactionMergers(transactionIdMergers.length);

        log.debug("Started ({}) background thread(s), of which ({}) transaction merger(s).", runningThreads.size(), state.getRunningTransactionMergers());
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public @NonNull Set<@NonNull NodeRef> getNextNodeIds(int amount) {
        Set<NodeRef> returnValue = Set.of();
        while (state.getRunningTransactionMergers() > 0) {
            returnValue = queuedNodes.takeFirst();
            if (returnValue.isEmpty()) {
                state.decrementRunningTransactionMergers();
                log.debug("Received an empty batch of NodeRefs, which indicate a halted transaction merger. Remaining transaction mergers is ({}).", state.getRunningTransactionMergers());
            } else break;
        }

        return returnValue;
    }

    @Override
    public void onStop() {
        log.debug("Stopping the TransactionAggregationIndexingStrategy.");
        for (Thread thread : runningThreads) thread.interrupt();
        runningThreads.clear();

        state.setCurrentTransactionId(-1);
        // I'm leaving maxTransactionId as-is. Gives a nice indicator where the previous iteration finished.
        cycleProgress.set(NullCycleProgress.getInstance());
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getState() {
        return state.getMapRepresentation();
    }

    @Override
    public @NonNull CycleProgress getCycleProgress() {
        return cycleProgress.get();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        state.bindTo(registry);
        transactionIdFetcher.bindTo(registry);

        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.queued-nodes", queuedNodes, BlockingDeque::size);
        registry.gauge("eu.xenit.alfresco.healthprocessor.indexing.threshold.running-background-threads", runningThreads, value -> value.stream().filter(Thread::isAlive).count());
    }
}