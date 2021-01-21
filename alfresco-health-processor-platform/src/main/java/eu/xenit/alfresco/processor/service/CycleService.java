package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.Cycle;
import eu.xenit.alfresco.processor.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class CycleService {
    private static final Logger logger = LoggerFactory.getLogger(CycleService.class);

    protected final RetryingTransactionHelper retryingTransactionHelper;

    public void execute(HealthProcessorConfiguration configurationService) {
        Cycle cycle = createCycle( configurationService);
        AtomicBoolean continueCycle = new AtomicBoolean();
        run(configurationService, cycle, continueCycle);
        while(configurationService.isEnabled()
                && !configurationService.isRunOnce()
                && continueCycle.get()) {
            run(configurationService, cycle, continueCycle);
        }
    }

    Cycle createCycle(HealthProcessorConfiguration configuration) {
        return new Cycle(
                configuration.getFirstTransaction(),
                configuration.getTransactionBatchSize(),
                configuration.getFirstTransaction(),
                configuration.getTimeIncrementSeconds(),
                configuration.getFirstCommitTime(),
                configuration.getFirstCommitTime()
        );
    }

    void run(HealthProcessorConfiguration configurationService, Cycle cycle, AtomicBoolean continueCycle) {
        start(cycle);

        AtomicBoolean reachedMaxTx = new AtomicBoolean(false);
        retryingTransactionHelper.doInTransaction(() -> {
            reachedMaxTx.set(reachedLastTx(cycle.getCurrentTransactionId()));
            return null;
        },false, true);

        if(reachedMaxTx.get()) {
            try {
                logger.error("Max transaction reached, entering idle state");
                Thread.sleep(configurationService.getTimeIncrementSeconds() * 1000);
            } catch (InterruptedException e) {
                logger.error("Idling has failed, aborting...", e);
                continueCycle.set(false);
            }
        }
    }

    void start(Cycle cycle) {
        long txnBatchSize = cycle.getTxnBatchSize();
        long timeIncrementSec = cycle.getTimeIncrementSeconds();

        logger.debug("Tracking changes ... Start commit time: {}",
                DateTimeUtil.toReadableString(cycle.getFirstCommitTime()));

        processTxnRange(cycle, txnBatchSize, timeIncrementSec);

        while(txnHistoryIsCatchingUp(timeIncrementSec,cycle.getCurrentCommitTimeMs())){
            cycle.setCurrentCommitTimeMs(cycle.getCurrentCommitTimeMs() + timeIncrementSec);
            processTxnRange(cycle, txnBatchSize, timeIncrementSec);
        }
    }

    void processTxnRange(Cycle cycle, long txnBatchSize, long timeIncrementSeconds) {
        // Save current progress in case of transactions collection failure
        long maxTxId = cycle.getCurrentTransactionId();
        long maxCommitTimeMs = cycle.getCurrentCommitTimeMs();

        try {
            List<Transaction> txs = getNodeTransactions(
                    txnBatchSize, timeIncrementSeconds);

            logger.debug("Found {} transactions", txs.size());

            if(txs.size() > 0) {
                // Yay, we have a list of transactions to process! Save them for later
                maxCommitTimeMs = txs.stream().map(Transaction::getCommitTimeMs)
                        .max(Long::compare).get();
                maxTxId = txs.stream().map(Transaction::getId)
                        .max(Long::compare).get();

                for(Transaction tx : txs) {
                    try {
                        retryingTransactionHelper.doInTransaction(() -> {
                            // do work
                            return null;
                        }, true, false);
                    } catch (Exception e) {
                        logger.error("Tracker loop failed: " + e.getMessage(), e);
                    }
                }
            }

            cycle.setCurrentCommitTimeMs(maxTxId);
            cycle.setCurrentCommitTimeMs( Long.max(maxCommitTimeMs, cycle.getCurrentCommitTimeMs()));

        } catch (Exception ex) {
            logger.error("Impossible to read tracker info: " + ex.getMessage(), ex);
        }
    }

    boolean txnHistoryIsCatchingUp(long timeIncrementSec, long commitTimeMs) {
        return DateTimeUtil.xSecondsAgoToMs(timeIncrementSec) > commitTimeMs;
    }

    private boolean reachedLastTx(long currentTransactionId) {
        return currentTransactionId < 100_000;
    }

    private List<Transaction> getNodeTransactions(long txnBatchSize, long    timeIncrementSeconds) {
        return new ArrayList<>();
    }
}
