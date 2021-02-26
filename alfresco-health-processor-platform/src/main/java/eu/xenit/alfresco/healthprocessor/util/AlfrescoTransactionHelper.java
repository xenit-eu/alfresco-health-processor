package eu.xenit.alfresco.healthprocessor.util;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.transaction.RetryingTransactionHelper;

@RequiredArgsConstructor
public class AlfrescoTransactionHelper implements TransactionHelper {

    private final RetryingTransactionHelper retryingTransactionHelper;

    @Override
    public <T> T inNewTransaction(Supplier<T> supplier, boolean readOnly) {
        return retryingTransactionHelper.doInTransaction(supplier::get, readOnly, true);
    }
}
