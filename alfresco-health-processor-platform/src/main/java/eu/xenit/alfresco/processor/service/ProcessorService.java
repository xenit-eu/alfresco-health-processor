package eu.xenit.alfresco.processor.service;

import lombok.AllArgsConstructor;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

@AllArgsConstructor
public class ProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorService.class);

    protected final RetryingTransactionHelper retryingTransactionHelper;
    protected final ExecutorService executorService;
    protected final HealthProcessorConfiguration configuration;
    protected final ProcessorAttributeService processorAttributeService;
    protected final CycleService cycleService;

    public void validateHealth() {
        if(!configuration.isEnabled()) {
            logger.info("Health validation initiated, but it is not enabled, aborting.");
            return;
        }

        if(processorAttributeService
                .getAttribute(ProcessorAttributeService.ATTR_KEY_IS_RUNNING, false)) {
            logger.info("Health validation initiated, but it is already running, aborting.");
            return;
        }

        executorService.submit(() -> {
            logger.trace("Current thread id: {}", Thread.currentThread().getId());
            doInTransaction(
                    () -> processorAttributeService
                                .persistAttribute(ProcessorAttributeService.ATTR_KEY_IS_RUNNING, true),
                    false, true);
            try {
                this.cycleService.execute(configuration);
            } finally {
                processorAttributeService.cleanupAttributes();
            }
        });
    }

    void doInTransaction(final Runnable cb, final boolean readOnly, final boolean requiresNew) {
        retryingTransactionHelper.doInTransaction(() -> {
            cb.run();
            return null;
        }, readOnly, requiresNew);
    }
}
