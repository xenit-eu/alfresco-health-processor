package eu.xenit.alfresco.healthprocessor.indexing.txnaggregation;

import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionAggregationIndexingStrategyTransactionIdFetcherTest {

    public static final @NonNull Pattern QUERY_PATTERN = Pattern.compile("SELECT txn\\.id as id FROM alf_transaction txn WHERE txn\\.id BETWEEN (\\d+) AND (\\d+) ORDER BY txn\\.id ASC LIMIT (\\d+)");    private static final int AMOUNT_OF_DUMMY_TRANSACTIONS = 1000;
    private static final int AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS = 5;
    private static final int TRANSACTIONS_BATCH_SIZE = 10;
    private static final @NonNull TransactionAggregationIndexingStrategyConfiguration CONFIGURATION =
            new TransactionAggregationIndexingStrategyConfiguration(AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS, TRANSACTIONS_BATCH_SIZE,
            -1, 0, AMOUNT_OF_DUMMY_TRANSACTIONS);

    private final @NonNull ArrayList<Long> dummyTransactionIDs = new ArrayList<>(AMOUNT_OF_DUMMY_TRANSACTIONS);
    private final @NonNull JdbcTemplate dummyJdbcTemplate = mock(JdbcTemplate.class);

    private TransactionAggregationIndexingStrategyTransactionIdFetcher fetcher;

    public TransactionAggregationIndexingStrategyTransactionIdFetcherTest() throws SQLException {
        LongStream.range(0, AMOUNT_OF_DUMMY_TRANSACTIONS).forEach(dummyTransactionIDs::add);

        when(dummyJdbcTemplate.queryForList(anyString(), eq(Long.class))).thenAnswer(invocation -> {
            Matcher matcher = QUERY_PATTERN.matcher(invocation.getArgument(0));
            if (!matcher.matches()) throw new IllegalArgumentException(String.format("unexpected query: %s", invocation.getArgument(0)));
            int startTxnId = Integer.parseInt(matcher.group(1));
            int endTxnId = Integer.parseInt(matcher.group(2));
            int limit = Integer.parseInt(matcher.group(3));

            int end = Math.min(startTxnId + limit, Math.min(endTxnId, AMOUNT_OF_DUMMY_TRANSACTIONS));
            return dummyTransactionIDs.subList(startTxnId, end);
        });
    }

    @BeforeEach
    void setUp() {
        TransactionAggregationIndexingStrategyState state = new TransactionAggregationIndexingStrategyState(0, AMOUNT_OF_DUMMY_TRANSACTIONS, AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS);
        fetcher = new TransactionAggregationIndexingStrategyTransactionIdFetcher(CONFIGURATION, dummyJdbcTemplate, state);
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
                    List<Long> transactionIDs = fetcher.getNextTransactionIDs();
                    assertEquals(dummyTransactionIDs.subList((i * AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS + j) * TRANSACTIONS_BATCH_SIZE,
                            (i * AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS + j + 1) * TRANSACTIONS_BATCH_SIZE), transactionIDs);
                }
            }

            // Every background worker receives the end signal.
            for (int j = 0; j < AMOUNT_OF_TRANSACTIONS_BACKGROUND_WORKERS; j ++) {
                assertTrue(fetcher.getNextTransactionIDs().isEmpty());
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
        TransactionAggregationIndexingStrategyState state = new TransactionAggregationIndexingStrategyState(0, 0, 0);
        TransactionAggregationIndexingStrategyConfiguration configurationOne = new TransactionAggregationIndexingStrategyConfiguration(0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new TransactionAggregationIndexingStrategyTransactionIdFetcher(configurationOne, dummyJdbcTemplate, state));
        TransactionAggregationIndexingStrategyConfiguration configurationTwo = new TransactionAggregationIndexingStrategyConfiguration(1, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new TransactionAggregationIndexingStrategyTransactionIdFetcher(configurationTwo, dummyJdbcTemplate, state));
    }

}