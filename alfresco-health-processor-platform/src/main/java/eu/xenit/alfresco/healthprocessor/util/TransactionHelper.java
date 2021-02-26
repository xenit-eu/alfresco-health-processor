package eu.xenit.alfresco.healthprocessor.util;

import java.util.function.Supplier;

public interface TransactionHelper {

    default void inNewTransaction(Runnable runnable, boolean readOnly) {
        inNewTransaction(() -> {
            runnable.run();
            return null;
        }, readOnly);
    }

    <T> T inNewTransaction(Supplier<T> supplier, boolean readOnly);
}
