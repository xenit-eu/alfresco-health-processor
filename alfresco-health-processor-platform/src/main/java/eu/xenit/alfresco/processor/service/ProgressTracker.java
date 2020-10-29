package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.TrackerInfo;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@AllArgsConstructor
public class ProgressTracker {
    private static final Logger logger = LoggerFactory.getLogger(ProgressTracker.class);

    protected final SOLRTrackingComponent tracker;

    public void updateTrackerInfo(TrackerInfo trackerInfo,
                                  final long maxTransactionId, final long maxCommitTime) {
        OffsetDateTime now = OffsetDateTime.now();
        logger.debug("Setting maxTransactionIdto {}", maxTransactionId);
        logger.debug("Setting maxCommitTime to {}", maxCommitTime);
        logger.debug("Setting timestamp to {}", now);
        logger.debug("Setting name to {}", this.getClass().getName());
        trackerInfo.setTransactionId(maxTransactionId);
        trackerInfo.setCommitTimeMs(maxCommitTime);
        trackerInfo.setTimestamp(now);
        trackerInfo.setName(this.getClass().getName());
    }

    public boolean reachedLastTx(TrackerInfo ti) {
        boolean result = Objects.equals(ti.getTransactionId(), tracker.getMaxTxnId());
        logger.info("Reached Last: {} ; provided Tx: {} ; max tx: {}",
                result, ti.getTransactionId(), tracker.getMaxTxnId());
        return result;
    }

    public TrackerInfo getTrackerInfo(final long firstTxn, final long firstCommitTime) {
        // If no progress is recorded, scan from the beginning
        TrackerInfo trackerInfo = new TrackerInfo();

        if(firstTxn > 0 && firstCommitTime > 0) {
            updateTrackerInfo(trackerInfo, firstTxn - 1, firstCommitTime);
        } else {
            updateTrackerInfo(trackerInfo,
                    1,
                    LocalDate.now()
                            .minusYears(20)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
        }
        return trackerInfo;
    }
}
