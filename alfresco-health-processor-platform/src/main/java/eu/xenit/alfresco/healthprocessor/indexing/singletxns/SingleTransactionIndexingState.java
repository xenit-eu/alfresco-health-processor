package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;


@Data
public class SingleTransactionIndexingState {

    public final static @NonNull String CURRENT_TXN_ID_IDENTIFIER = "current-txn-id";
    public final static @NonNull String LAST_TXN_ID_IDENTIFIER = "last-txn-id";
    public final static @NonNull String CURRENTLY_PROCESSED_TXN_ID_IDENTIFIER = "currently-processed-txn-id";
    public final static @NonNull String CURRENTLY_BACKGROUND_WORKER_QUEUE_SIZE_IDENTIFIER = "currently-background-worker-queue-size";

    long currentTxnId = -1;
    long lastTxnId;
    long currentlyProcessedTxnId = -1;
    int currentlyBackgroundWorkerQueueSize = 0;

    public @NonNull Map<String, String> generateMapRepresentation() {
        return Map.of(CURRENT_TXN_ID_IDENTIFIER, Long.toString(currentTxnId),
                LAST_TXN_ID_IDENTIFIER, Long.toString(lastTxnId),
                CURRENTLY_PROCESSED_TXN_ID_IDENTIFIER, Long.toString(currentlyProcessedTxnId),
                CURRENTLY_BACKGROUND_WORKER_QUEUE_SIZE_IDENTIFIER, Integer.toString(currentlyBackgroundWorkerQueueSize));
    }

}
