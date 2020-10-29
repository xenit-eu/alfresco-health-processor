package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.Cycle;
import eu.xenit.alfresco.processor.model.TrackerInfo;
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
    protected final ProgressTracker progressTracker;

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
        TrackerInfo ti = progressTracker.getTrackerInfo(
                configurationService.getFirstTransaction(),
                configurationService.getFirstCommitTime()
        );
        return new Cycle(configurationService.getTransactionLimit(),
                configurationService.getFirstTransaction(),
                configurationService.getTimeIncrementSeconds(),
                configurationService.getFirstCommitTime(),
                ti
        );
    }

    void run(HealthProcessorConfiguration configurationService, Cycle cycle, AtomicBoolean continueCycle) {
        cycle = start(cycle);

        final TrackerInfo ti = cycle.getTrackerInfo();
        AtomicBoolean reachedMaxTx = new AtomicBoolean(false);
        retryingTransactionHelper.doInTransaction(() -> {
            reachedMaxTx.set(progressTracker.reachedLastTx(ti));
            return null;
        },false, true);

        if(reachedMaxTx.get()) {
            try {
                logger.error("Max transaction reached, entering idle state");
                Thread.sleep(configurationService.getTimeIncrementSeconds() * 1000);
            } catch (InterruptedException e) {
                logger.error("Idling has failed, aborting...");
                continueCycle.set(false);
            }
        }
    }

    Cycle start(Cycle cycle) {
        TrackerInfo trackerInfo = cycle.getTrackerInfo();
        int txnLimit = cycle.getTxnLimit();
        int timeIncrementSeconds = cycle.getTimeIncrementSeconds();
        long firstCommitTime = cycle.getFirstCommitTime();

        logger.debug("Tracking changes ... Start commit time: {}", LocalDateTime
                .ofInstant(Instant.ofEpochMilli(firstCommitTime), ZoneId.systemDefault())
                .toString());

        trackerInfo = processTxnRange(
                trackerInfo, txnLimit, timeIncrementSeconds);

        long timeIncrementEpoch = timeIncrementSeconds * 1000L;
        while(txnHistoryIsCatchingUp(timeIncrementEpoch, trackerInfo)) {
            progressTracker.updateTrackerInfo(
                    trackerInfo,
                    trackerInfo.getTransactionId(),
                    trackerInfo.getCommitTimeMs() + timeIncrementEpoch);
            trackerInfo = processTxnRange(
                    trackerInfo, txnLimit, timeIncrementSeconds);
        }

        cycle.setTrackerInfo(trackerInfo);
        return cycle;
    }

    private boolean txnHistoryIsCatchingUp(long timeIncrementEpoch, TrackerInfo trackerInfo) {
        long supposedLastScanTime = OffsetDateTime.now().toInstant().toEpochMilli() - timeIncrementEpoch;
        return supposedLastScanTime > trackerInfo.getCommitTimeMs();
    }

    TrackerInfo processTxnRange(TrackerInfo trackerInfo, int txnLimit, int timeIncrementSeconds) {
        // Save current progress in case of transactions collection failure
        long maxTxId = trackerInfo.getTransactionId();
        long maxCommitTimeMs = trackerInfo.getCommitTimeMs();

        try {
            List<Transaction> txs = getNodeTransactions(trackerInfo,
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

            progressTracker.updateTrackerInfo(trackerInfo, maxTxId,
                    Long.max(maxCommitTimeMs, trackerInfo.getCommitTimeMs()));

        } catch (Exception ex) {
            logger.error("Impossible to read tracker info: " + ex.getMessage(), ex);
        }
        return trackerInfo;
    }

    private List<Transaction> getNodeTransactions(TrackerInfo trackerInfo, int txnLimit, int timeIncrementSeconds) {
        return new ArrayList<>();
    }
}