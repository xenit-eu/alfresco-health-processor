package eu.xenit.alfresco.processor.service;

import lombok.Getter;
import lombok.Setter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessorServiceImpl implements ProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorServiceImpl.class);

    @Getter @Setter
    protected RetryingTransactionHelper retryingTransactionHelper;

    @Getter @Setter
    protected ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Getter @Setter
    protected HealthProcessorConfiguration configurationService;

    protected  ProcessorServiceImpl() {}
    public ProcessorServiceImpl(RetryingTransactionHelper retryingTransactionHelper,
                                ExecutorService executorService,
                                HealthProcessorConfiguration configurationService) {
        this.retryingTransactionHelper = retryingTransactionHelper;
        this.executorService = executorService;
        this.configurationService = configurationService;
    }

    @Override
    public void validateHealth() {
        if(!configurationService.isEnabled()) {
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
