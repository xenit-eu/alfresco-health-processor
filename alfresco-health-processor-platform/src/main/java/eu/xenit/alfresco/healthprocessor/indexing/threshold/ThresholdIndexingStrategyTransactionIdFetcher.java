package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


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
        try {
            long currentTransactionId = state.getCurrentTransactionId();
            long maxTransactionId = state.getMaxTransactionId();
            /*
                Problem: the health processor platform uses the transaction IDs to determine the initial transaction ID to start fetching nodes from.
                To not break this convention, we did the same.
                However, internally, we use timestamps for fetching nodes.

                Solution: convert the currentTransactionId to a timestamp, and use timestamps from there on.
             */
            long currentTimestamp = convertTransactionIdToTimestamp(currentTransactionId);
            int amountOfTransactionsToFetch = configuration.getTransactionsBackgroundWorkers() * configuration.getTransactionsBatchSize();

            List<Transaction> fetchedTransactions;
            do {
                fetchedTransactions = searchTrackingComponent.getTransactions(currentTransactionId, currentTimestamp,
                        maxTransactionId, Long.MAX_VALUE, amountOfTransactionsToFetch);
                if (fetchedTransactions.isEmpty()) break;

                queueTransactions(fetchedTransactions);
                currentTimestamp = fetchedTransactions.get(fetchedTransactions.size() - 1).getCommitTimeMs();
                currentTransactionId = fetchedTransactions.get(fetchedTransactions.size() - 1).getId() + 1;
            } while (currentTransactionId < maxTransactionId); // maxTransactionId is exclusive.
        } finally {
            signalEnd();
        }
    }

    private void signalEnd() {
        // Signal to each of the transaction mergers that the end has been reached.
        for (int i = 0; i < configuration.getTransactionsBatchSize(); i++) queuedTransactions.addLast(List.of());
    }

    public @NonNull List<Transaction> getNextTransactions() throws InterruptedException {
        List<Transaction> transactions = queuedTransactions.takeFirst();
        if (!transactions.isEmpty()) state.getTransactionBatchesQueueSize().decrementAndGet();
        return transactions;
    }

    private void queueTransactions(@NonNull List<Transaction> transactions) {
        int transactionsSize = transactions.size();

        for (int i = 0; i < configuration.getTransactionsBackgroundWorkers(); i ++) {
            List<Transaction> workerBatch = transactions.subList(i * configuration.getTransactionsBatchSize(),
                    Math.min((i + 1) * configuration.getTransactionsBatchSize(), transactionsSize));

            if (!workerBatch.isEmpty()) {
                queuedTransactions.addLast(workerBatch);
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