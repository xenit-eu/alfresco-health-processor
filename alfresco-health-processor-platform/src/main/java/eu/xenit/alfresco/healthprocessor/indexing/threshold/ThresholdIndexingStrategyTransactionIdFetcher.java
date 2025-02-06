package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.Transaction;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * In case the performance ever becomes an issue: this part can be easily multi-threaded too,
 * by dividing the transaction range into multiple parts and fetching them in parallel (using multiple fetchers).
 * However, this is not necessary at the moment. We're already at 200.000 transactions per minute on an underpowered laptop.
 * I'm just leaving this note here, in case this might help another developer in the future.
 */
@Slf4j
public class ThresholdIndexingStrategyTransactionIdFetcher implements Runnable {

    private final @NonNull BlockingDeque<@NonNull List<@NonNull Transaction>> queuedTransactions;

    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull ThresholdIndexingStrategyState state;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;

    public ThresholdIndexingStrategyTransactionIdFetcher(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                                         @NonNull SearchTrackingComponent searchTrackingComponent,
                                                         @NonNull ThresholdIndexingStrategyState state) {
        // No more required than the amount of background workers.
        // If the queue is full, it means that the background workers can not keep up with the transaction fetcher anyway.
        // Slow down in this case.
        this(configuration, searchTrackingComponent, state, new LinkedBlockingDeque<>(configuration.getTransactionsBackgroundWorkers()));
    }

    ThresholdIndexingStrategyTransactionIdFetcher(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                                  @NonNull SearchTrackingComponent searchTrackingComponent,
                                                  @NonNull ThresholdIndexingStrategyState state,
                                                  @NonNull LinkedBlockingDeque<@NonNull List<@NonNull Transaction>> queuedTransactions) {
        if (configuration.getTransactionsBackgroundWorkers() <= 0)
            throw new IllegalArgumentException(String.format("The amount of background workers must be greater than zero (%d provided).", configuration.getTransactionsBackgroundWorkers()));
        if (configuration.getTransactionsBatchSize() <= 0)
            throw new IllegalArgumentException(String.format("The batch size must be greater than zero (%d provided).", configuration.getTransactionsBatchSize()));

        this.searchTrackingComponent = searchTrackingComponent;
        this.state = state;
        this.configuration = configuration;
        this.queuedTransactions = queuedTransactions;
    }

    @Override
    public void run() {
        log.debug("Starting the ThresholdIndexingStrategyTransactionIdFetcher.");
        try {
            long currentTransactionId = state.getCurrentTransactionId();
            long maxTransactionId = state.getMaxTransactionId();
            int amountOfTransactionsToFetch = configuration.getTransactionsBackgroundWorkers() * configuration.getTransactionsBatchSize();

            List<Transaction> fetchedTransactions;
            do {
                log.trace("Fetching transactions from ({}) to ({}).", currentTransactionId, Math.min(currentTransactionId + amountOfTransactionsToFetch, maxTransactionId));
                fetchedTransactions = searchTrackingComponent.getTransactions(currentTransactionId, Long.MIN_VALUE,
                        maxTransactionId, Long.MAX_VALUE, amountOfTransactionsToFetch);
                log.trace("Fetched ({}) transactions.", fetchedTransactions.size());
                if (fetchedTransactions.isEmpty()) break;

                queueTransactions(fetchedTransactions);
                currentTransactionId = fetchedTransactions.get(fetchedTransactions.size() - 1).getId() + 1;
                state.setCurrentTransactionId(currentTransactionId); // UI update; nice to have.
            } while (currentTransactionId < maxTransactionId); // maxTransactionId is exclusive.
        } catch (InterruptedException e) {
            log.warn("The ThresholdIndexingStrategyTransactionIdFetcher has been interrupted. This is unexpected behavior. " +
                    "Trying to recover by signaling the end to the transaction merger(s).", e);
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
        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i++) queuedTransactions.putLast(List.of());
    }

    public @NonNull List<Transaction> getNextTransactions() throws InterruptedException {
        List<Transaction> transactions = queuedTransactions.takeFirst();
        if (!transactions.isEmpty()) state.getTransactionBatchesQueueSize().decrementAndGet();
        else log.trace("One of the transaction mergers is receiving the end signal from the transaction fetcher.");
        return transactions;
    }

    private void queueTransactions(@NonNull List<Transaction> transactions) throws InterruptedException {
        int transactionsSize = transactions.size();

        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i ++) {
            List<Transaction> workerBatch = transactions.subList(i * configuration.getTransactionsBatchSize(),
                    Math.min((i + 1) * configuration.getTransactionsBatchSize(), transactionsSize));

            if (!workerBatch.isEmpty()) {
                log.trace("Queuing a batch of ({}) transactions for transaction merger ({}).", workerBatch.size(), i);
                queuedTransactions.putLast(workerBatch);
                state.getTransactionBatchesQueueSize().incrementAndGet();
            }
            if (workerBatch.size() < configuration.getTransactionsBatchSize()) return;
        }
    }

}