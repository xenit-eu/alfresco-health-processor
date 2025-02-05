package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.Transaction;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


@Slf4j
public class ThresholdIndexingStrategyTransactionIdFetcher implements Runnable {

    private final @NonNull BlockingDeque<@NonNull List<@NonNull Transaction>> queuedTransactions;

    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull ThresholdIndexingStrategyState state;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration;

    public ThresholdIndexingStrategyTransactionIdFetcher(@NonNull ThresholdIndexingStrategyConfiguration configuration,
                                                         @NonNull SearchTrackingComponent searchTrackingComponent,
                                                         @NonNull ThresholdIndexingStrategyState state) {
        this.queuedTransactions = new LinkedBlockingDeque<>(configuration.getTransactionsBackgroundWorkers());

        this.searchTrackingComponent = searchTrackingComponent;
        this.state = state;
        this.configuration = configuration;
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
            } while (currentTransactionId < maxTransactionId); // maxTransactionId is exclusive.
        } catch (InterruptedException e) {
            log.warn("The ThresholdIndexingStrategyTransactionIdFetcher has been interrupted. This is unexpected behavior. " +
                    "Trying to recover by signaling the end to the transaction merger(s).", e);
        } finally {
            try {
                signalEnd();
            } catch (InterruptedException e) {
                log.error("The ThresholdIndexingStrategyTransactionIdFetcher has been interrupted while signaling the end to the transaction merger(s).", e);
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

    private long convertTransactionIdToTimestamp(long transactionId) throws IllegalArgumentException {
        List<Transaction> foundTransaction = searchTrackingComponent.getTransactions(transactionId, Long.MIN_VALUE,
                transactionId + 1, Long.MAX_VALUE, 1);
        if (foundTransaction.size() != 1) throw new IllegalArgumentException(String.format("Expected to find one transaction " +
                "with ID (%d), but found (%d).", transactionId, foundTransaction.size()));
        return foundTransaction.get(0).getCommitTimeMs();
    }
}