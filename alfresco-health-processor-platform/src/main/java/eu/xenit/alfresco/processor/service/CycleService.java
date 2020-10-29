package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.Cycle;
import eu.xenit.alfresco.processor.model.TrackerInfo;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class CycleService  {
    private static final Logger logger = LoggerFactory.getLogger(CycleService.class);

    protected final RetryingTransactionHelper retryingTransactionHelper;
    protected final ProgressTracker progressTracker;
    protected final NodeTxService nodeTxService;
    protected final ValidationService validationService;

    public void execute(HealthProcessorConfiguration configurationService) {
        Cycle cycle = createCycle( configurationService);
        AtomicBoolean continueCycle = new AtomicBoolean(true);
        run(configurationService, cycle, continueCycle);
        while(configurationService.isEnabled()
                && !configurationService.isRunOnce()
                && continueCycle.get()) {
            logger.trace("Restarting the cycle...");
            run(configurationService, cycle, continueCycle);
        }
        logger.trace("Cycle completed.");
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
        start(cycle);

        final TrackerInfo ti = cycle.getTrackerInfo();
        AtomicBoolean reachedMaxTx = new AtomicBoolean(false);
        retryingTransactionHelper.doInTransaction(() -> {
            reachedMaxTx.set(progressTracker.reachedLastTx(ti));
            return null;
        },false, true);

        if(reachedMaxTx.get()) {
            try {
                logger.debug("Max transaction reached, entering idle state");
                Thread.sleep(configurationService.getTimeIncrementSeconds() * 1000);
            } catch (InterruptedException e) {
                logger.error("Idling has failed, aborting...");
                continueCycle.set(false);
            }
        }
    }

    void start(Cycle cycle) {
        TrackerInfo trackerInfo = cycle.getTrackerInfo();
        int txnLimit = cycle.getTxnLimit();
        int timeIncrementSeconds = cycle.getTimeIncrementSeconds();
        long firstCommitTime = cycle.getFirstCommitTime();

        logger.trace("Tracking changes ... Start commit time: {}", LocalDateTime
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
    }

    private boolean txnHistoryIsCatchingUp(long timeIncrementEpoch, TrackerInfo trackerInfo) {
        long supposedLastScanTime = OffsetDateTime.now().toInstant().toEpochMilli() - timeIncrementEpoch;
        logger.trace("supposedLastScanTime {} > trackerInfo.getCommitTimeMs() {}",
                supposedLastScanTime , trackerInfo.getCommitTimeMs());
        return supposedLastScanTime > trackerInfo.getCommitTimeMs();
    }

    TrackerInfo processTxnRange(TrackerInfo trackerInfo, int txnLimit, int timeIncrementSeconds) {
        // Save current progress in case of transactions collection failure
        long maxTxId = trackerInfo.getTransactionId();
        long maxCommitTimeMs = trackerInfo.getCommitTimeMs();

        logger.debug("maxTxId {}", maxTxId);
        logger.debug("maxCommitTimeMs {}", maxCommitTimeMs);

        try {
            List<Transaction> txs = nodeTxService.getNodeTransactions(
                    trackerInfo, txnLimit, timeIncrementSeconds);

            logger.debug("Found {} transactions", txs.size());

            if(txs.size() > 0) {
                // Yay, we have a list of transactions to process! Save them for later
                maxCommitTimeMs = txs.stream().map(Transaction::getCommitTimeMs)
                        .max(Long::compare).get();
                maxTxId = txs.stream().map(Transaction::getId)
                        .max(Long::compare)
                        .orElse(-1L);

                for(Transaction tx : txs) {
                    try {
                        retryingTransactionHelper.doInTransaction(() -> {
                            // Collect node references within transaction range
                            List<NodeRef> nodes = nodeTxService.getNodeReferences(tx);
                            validationService.validate(nodes);
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
}
