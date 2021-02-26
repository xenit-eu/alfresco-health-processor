package eu.xenit.alfresco.healthprocessor.util;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface TransactionHelper {

    default void inNewTransaction(Runnable runnable, boolean readOnly) {
        inTransaction(runnable, readOnly, true);
    }

    default <T> T inNewTransaction(Supplier<T> supplier, boolean readOnly) {
        return inTransaction(supplier, readOnly, true);
    }

    default void inTransaction(Runnable runnable, boolean readOnly) {
        inTransaction(runnable, readOnly, false);
    }

    default <T> T inTransaction(Supplier<T> supplier, boolean readOnly) {
        return inTransaction(supplier, readOnly, false);
    }

    default void inTransaction(Runnable runnable, boolean readOnly, boolean requiresNew) {
        inTransaction(() -> {
            runnable.run();
            return null;
        }, readOnly, requiresNew);
    }

    <T> T inTransaction(Supplier<T> supplier, boolean readOnly, boolean requiresNew);
}
