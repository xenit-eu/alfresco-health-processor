package eu.xenit.alfresco.processor.model;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Information object to save the state of the tracking activity. The state is persisted to allow for the correct
 * continuation after a restart of the system.
 */
@Data
public class TrackerInfo {
    /**
     * Timestamp of the last action
     */
    private OffsetDateTime timestamp;
    /**
     * Id of the last transaction processed by the tracker
     */
    private Long transactionId;
    /**
     * Highest commit time in ms of processed transaction
     */
    private Long commitTimeMs = -1L;
    /**
     * Name of the artifact running this tracker software
     */
    private String name;
}
