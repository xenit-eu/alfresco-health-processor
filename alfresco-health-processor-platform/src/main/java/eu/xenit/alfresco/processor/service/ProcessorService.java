package eu.xenit.alfresco.processor.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class ProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorService.class);

    public enum TransactionScope {
        ALL,
        NEW
    }

    @Setter(AccessLevel.PACKAGE)
    protected RetryingTransactionHelper retryingTransactionHelper;

    @Setter(AccessLevel.PACKAGE)
    protected ExecutorService executorService;

    @Setter(AccessLevel.PACKAGE)
    protected HealthProcessorConfiguration configuration;

    public void validateHealth() {
        if(!configuration.isEnabled()) {
            logger.info("Health validation initiated, but it is not enabled, aborting.");
            return;
        }

        executorService.submit(() -> {
            logger.trace("Current thread id: {}", Thread.currentThread().getId());
            doInTransaction(
                    () -> {
                      // do work
                    }, false, true);
        });
    }

    void doInTransaction(final Runnable cb, final boolean readOnly, final boolean requiresNew) {
        retryingTransactionHelper.doInTransaction(() -> {
            cb.run();
            return null;
        }, readOnly, requiresNew);
    }
}
