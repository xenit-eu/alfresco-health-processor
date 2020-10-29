package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.Cycle;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    Cycle createCycle(HealthProcessorConfiguration configurationService) {
        return new Cycle(
                configurationService.getTransactionLimit(),
                configurationService.getFirstTransaction(),
                configurationService.getTimeIncrementSeconds(),
                configurationService.getFirstCommitTime(),
                configurationService.getFirstTransaction(),
                configurationService.getFirstCommitTime()
        );
    }

    void run(HealthProcessorConfiguration configurationService, Cycle cycle, AtomicBoolean continueCycle) {
        start(cycle);

        AtomicBoolean reachedMaxTx = new AtomicBoolean(false);
        retryingTransactionHelper.doInTransaction(() -> {
            reachedMaxTx.set(reachedLastTx(cycle.getTransactionId()));
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
        int txnLimit = cycle.getTxnLimit();
        int timeIncrementSeconds = cycle.getTimeIncrementSeconds();
        long firstCommitTime = cycle.getFirstCommitTime();

        logger.debug("Tracking changes ... Start commit time: {}", LocalDateTime
                .ofInstant(Instant.ofEpochMilli(firstCommitTime), ZoneId.systemDefault())
                .toString());

        processTxnRange(cycle, txnLimit, timeIncrementSeconds);

        long timeIncrementEpoch = timeIncrementSeconds * 1000L;
        while(txnHistoryIsCatchingUp(timeIncrementEpoch, cycle.getCommitTimeMs())) {
            cycle.setCommitTimeMs(cycle.getCommitTimeMs() + timeIncrementEpoch);
            processTxnRange(cycle, txnLimit, timeIncrementSeconds);
        }
    }

    void processTxnRange(Cycle cycle, int txnLimit, int timeIncrementSeconds) {
        // Save current progress in case of transactions collection failure
        long maxTxId = cycle.getTransactionId();
        long maxCommitTimeMs = cycle.getCommitTimeMs();

        try {
            List<Transaction> txs = getNodeTransactions(
                    txnLimit, timeIncrementSeconds);

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

            cycle.setTransactionId(maxTxId);
            cycle.setCommitTimeMs( Long.max(maxCommitTimeMs, cycle.getCommitTimeMs()));

        } catch (Exception ex) {
            logger.error("Impossible to read tracker info: " + ex.getMessage(), ex);
        }
    }

    private boolean txnHistoryIsCatchingUp(long timeIncrementEpoch, long commitTimeMs) {
        long supposedLastScanTime = OffsetDateTime.now().toInstant().toEpochMilli() - timeIncrementEpoch;
        return supposedLastScanTime > commitTimeMs;
    }

    private boolean reachedLastTx(long transactionId) {
        return transactionId < 100_000;
    }

    private List<Transaction> getNodeTransactions(int txnLimit, int timeIncrementSeconds) {
        return new ArrayList<>();
    }
}
