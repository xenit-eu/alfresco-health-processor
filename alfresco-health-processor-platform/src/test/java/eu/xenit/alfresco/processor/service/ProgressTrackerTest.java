package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.model.TrackerInfo;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProgressTrackerTest {
    @Mock
    private SOLRTrackingComponent tracker;

    @Test
    public void updateTrackerInfoTest() {
        final long value = 123L;
        final TrackerInfo trackerInfo = new TrackerInfo();
        new ProgressTracker(tracker)
                .updateTrackerInfo(trackerInfo,value,value);
        validateTrackerInfo(trackerInfo, value, value);
    }

    @Test
    public void createNewTrackerInfoTest() {
        final long value = 234L;
        final ProgressTracker progressTracker = new ProgressTracker(tracker);
        final TrackerInfo trackerInfo = progressTracker
                .getTrackerInfo(value, value);
        validateTrackerInfo(trackerInfo, value - 1, value);

        // Idempotency test
        final TrackerInfo newTrackerInfo = progressTracker
                .getTrackerInfo(value, value);
        validateTrackerInfo(newTrackerInfo, value - 1, value);
    }

    @Test
    public void reachedLastTxTest() {
        final long value = 123L;
        when(tracker.getMaxTxnId()).thenAnswer(invocation -> value);
        final ProgressTracker progressTracker = new ProgressTracker(tracker);
        final TrackerInfo trackerInfo = new TrackerInfo();
        trackerInfo.setTransactionId(value);
        assertTrue(progressTracker.reachedLastTx(trackerInfo));
    }

    @Test
    public void notReachedLastTxTest() {
        when(tracker.getMaxTxnId()).thenAnswer(invocation -> 123L);
        final ProgressTracker progressTracker = new ProgressTracker(tracker);
        final TrackerInfo trackerInfo = new TrackerInfo();
        trackerInfo.setTransactionId(456L);
        assertFalse(progressTracker.reachedLastTx(trackerInfo));
    }

    private void validateTrackerInfo(final TrackerInfo ti, final long txId, final long commitTimeMs) {
        assertNotNull(ti.getName());
        assertNotNull(ti.getClass());
        assertNotNull(ti.getTimestamp());
        assertNotNull(ti.getTransactionId());
        assertNotNull(ti.getCommitTimeMs());
        assertEquals(txId, ti.getTransactionId());
        assertEquals(commitTimeMs, ti.getCommitTimeMs());
    }
}
