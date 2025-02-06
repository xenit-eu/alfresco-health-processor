package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.alfresco.repo.solr.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThresholdIndexingStrategyTransactionIdFetcherTest {

    private final static int AMOUNT_OF_DUMMY_TRANSACTIONS = 1000;
    private final static int AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS = 5;
    private final static int TRANSACTIONS_BATCH_SIZE = 10;
    private final static @NonNull ThresholdIndexingStrategyConfiguration CONFIGURATION =
            new ThresholdIndexingStrategyConfiguration(AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS, TRANSACTIONS_BATCH_SIZE,
            -1, 0, AMOUNT_OF_DUMMY_TRANSACTIONS);

    private final @NonNull ArrayList<Transaction> dummyTransactions = new ArrayList<>();
    private final @NonNull SearchTrackingComponent dummySearchTrackingComponent = mock(SearchTrackingComponent.class);

    private ThresholdIndexingStrategyTransactionIdFetcher fetcher;

    public ThresholdIndexingStrategyTransactionIdFetcherTest() {
        IntStream.range(0, AMOUNT_OF_DUMMY_TRANSACTIONS)
                .forEach(i -> {
                    Transaction dummyTransaction = mock(Transaction.class);
                    when(dummyTransaction.getId()).thenReturn((long) i);
                    dummyTransactions.add(dummyTransaction);
                });
        when(dummySearchTrackingComponent.getTransactions(anyLong(), eq(Long.MIN_VALUE), anyLong(),
                eq(Long.MAX_VALUE), anyInt())).thenAnswer(invocation -> {
            long fromIndex = invocation.getArgument(0);
            long toIndex = invocation.getArgument(2);
            int amount = invocation.getArgument(4);

            return dummyTransactions.subList((int) fromIndex, Math.min((int) fromIndex + amount, Math.min((int) toIndex, dummyTransactions.size())));
        });
    }

    @BeforeEach
    void setUp() {
        ThresholdIndexingStrategyState state = new ThresholdIndexingStrategyState(0, AMOUNT_OF_DUMMY_TRANSACTIONS, AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS);
        fetcher = new ThresholdIndexingStrategyTransactionIdFetcher(CONFIGURATION, dummySearchTrackingComponent, state);
    }

    @Test
    void run() throws InterruptedException {
        Thread thread = new Thread(fetcher);
        thread.start();
        try {

            // All transactions are processed.
            int amountOfBatchesForEachBackGroundWorker = AMOUNT_OF_DUMMY_TRANSACTIONS / (AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS * TRANSACTIONS_BATCH_SIZE);
            for (int i = 0; i < amountOfBatchesForEachBackGroundWorker; i ++) {
                for (int j = 0; j < AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS; j++) {
                    List<org.alfresco.repo.solr.Transaction> transactions = fetcher.getNextTransactions();
                    assertEquals(dummyTransactions.subList((i * AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS + j) * TRANSACTIONS_BATCH_SIZE,
                            (i * AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS + j + 1) * TRANSACTIONS_BATCH_SIZE), transactions);
                }
            }

            // Every background worker receives the end signal.
            for (int j = 0; j < AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS; j ++) {
                assertTrue(fetcher.getNextTransactions().isEmpty());
            }

            // The fetcher kills itself (sound a lot darker than it is).
            thread.join(3_000);
            assertFalse(thread.isAlive());
        } finally {
            thread.interrupt();
        }
    }

    @Test
    public void testArguments() {
        ThresholdIndexingStrategyConfiguration configurationOne = new ThresholdIndexingStrategyConfiguration(0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ThresholdIndexingStrategyTransactionIdFetcher(configurationOne, dummySearchTrackingComponent, new ThresholdIndexingStrategyState(0, 0, 0)));
        ThresholdIndexingStrategyConfiguration configurationTwo = new ThresholdIndexingStrategyConfiguration(1, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ThresholdIndexingStrategyTransactionIdFetcher(configurationTwo, dummySearchTrackingComponent, new ThresholdIndexingStrategyState(0, 0, 0)));
    }

}