package eu.xenit.alfresco.healthprocessor.indexing.singletxns;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;


@Data
public class SingleTransactionIndexingState {

    public final static @NonNull String CURRENT_TXN_ID_IDENTIFIER = "current-txn-id";
    public final static @NonNull String LAST_TXN_ID_IDENTIFIER = "last-txn-id";

    long currentTxnId = -1;
    long lastTxnId;

    public @NonNull Map<String, String> generateMapRepresentation() {
        return Map.of(CURRENT_TXN_ID_IDENTIFIER, Long.toString(currentTxnId),
                LAST_TXN_ID_IDENTIFIER, Long.toString(lastTxnId));
    }

}
