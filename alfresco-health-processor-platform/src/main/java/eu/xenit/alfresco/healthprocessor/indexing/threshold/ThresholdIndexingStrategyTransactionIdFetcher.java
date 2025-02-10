package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

// TODO: add an explanation about why we directly contact the DB.
@Slf4j
public class ThresholdIndexingStrategyTransactionIdFetcher implements Runnable {

    private final static @NonNull String QUERY = "SELECT txn.id as id FROM alf_transaction txn WHERE txn.id BETWEEN %d AND %d ORDER BY txn.id ASC LIMIT %d";

    private final @NonNull BlockingDeque<@NonNull List<@NonNull Long>> queuedTransactionIDs;

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final @NonNull ThresholdIndexingStrategyState state;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;

    public ThresholdIndexingStrategyTransactionIdFetcher(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                                         @NonNull DataSource dataSource,
                                                         @NonNull ThresholdIndexingStrategyState state) {
        // No more required than the amount of background workers.
        // If the queue is full, it means that the background workers can not keep up with the transaction fetcher anyway.
        // Slow down in this case.
        this(configuration, dataSource, state, new LinkedBlockingDeque<>(configuration.getTransactionsBackgroundWorkers()));
    }

    ThresholdIndexingStrategyTransactionIdFetcher(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                                  @NonNull DataSource dataSource,
                                                  @NonNull ThresholdIndexingStrategyState state,
                                                  @NonNull LinkedBlockingDeque<@NonNull List<@NonNull Long>> queuedTransactionIDs) {
        if (configuration.getTransactionsBackgroundWorkers() <= 0)
            throw new IllegalArgumentException(String.format("The amount of background workers must be greater than zero (%d provided).", configuration.getTransactionsBackgroundWorkers()));
        if (configuration.getTransactionsBatchSize() <= 0)
            throw new IllegalArgumentException(String.format("The batch size must be greater than zero (%d provided).", configuration.getTransactionsBatchSize()));

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.state = state;
        this.configuration = configuration;
        this.queuedTransactionIDs = queuedTransactionIDs;
    }

    @Override
    public void run() {
        log.debug("Starting the ThresholdIndexingStrategyTransactionIdFetcher.");
        try {
            long currentTransactionId = state.getCurrentTransactionId();
            long maxTransactionId = state.getMaxTransactionId();
            int amountOfTransactionsToFetch = configuration.getTransactionsBackgroundWorkers() * configuration.getTransactionsBatchSize();

            List<Long> fetchedTransactionsIDs;
            do {
                String query = String.format(QUERY, currentTransactionId, maxTransactionId, amountOfTransactionsToFetch);
                log.trace("Fetching transactions from ({}) to ({}).", currentTransactionId, Math.min(currentTransactionId + amountOfTransactionsToFetch, maxTransactionId));
                fetchedTransactionsIDs = jdbcTemplate.queryForList(query, Long.class);
                log.trace("Fetched ({}) transaction ID(s).", fetchedTransactionsIDs.size());
                if (fetchedTransactionsIDs.isEmpty()) break;

                queueTransactions(fetchedTransactionsIDs);
                currentTransactionId = fetchedTransactionsIDs.get(fetchedTransactionsIDs.size() - 1) + 1;
                state.setCurrentTransactionId(currentTransactionId); // UI update; nice to have.
            } while (currentTransactionId < maxTransactionId); // maxTransactionId is exclusive.
        } catch (Exception e) {
            log.warn("An exception occurred while fetching transactions. Trying to signal the end to the transaction merger(s).", e);
        } finally {
            try {
                signalEnd();
            } catch (InterruptedException e) {
                log.error("The ThresholdIndexingStrategyTransactionIdFetcher has been interrupted while signaling the end to the transaction merger(s). " +
                        "The threshold indexer can not recover from this.", e);
            }
        }
    }

    private void signalEnd() throws InterruptedException {
        // Signal to each of the transaction mergers that the end has been reached.
        log.trace("Signaling the end to ({}) transaction merger(s).", configuration.getTransactionsBackgroundWorkers());
        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i++) queuedTransactionIDs.putLast(List.of());
    }

    public @NonNull List<@NonNull Long> getNextTransactionIDs() throws InterruptedException {
        List<Long> transactionIDs = queuedTransactionIDs.takeFirst();
        if (!transactionIDs.isEmpty()) state.getTransactionBatchesQueueSize().decrementAndGet();
        else log.trace("One of the transaction mergers is receiving the end signal from the transaction fetcher.");
        return transactionIDs;
    }

    private void queueTransactions(@NonNull List<@NonNull Long> transactionIDs) throws InterruptedException {
        int transactionsSize = transactionIDs.size();

        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i ++) {
            List<Long> workerBatch = transactionIDs.subList(i * configuration.getTransactionsBatchSize(),
                    Math.min((i + 1) * configuration.getTransactionsBatchSize(), transactionsSize));

            if (!workerBatch.isEmpty()) {
                log.trace("Queuing a batch of ({}) transactions for transaction merger ({}).", workerBatch.size(), i);
                queuedTransactionIDs.putLast(workerBatch);
                state.getTransactionBatchesQueueSize().incrementAndGet();
            }
            if (workerBatch.size() < configuration.getTransactionsBatchSize()) return;
        }
    }

}