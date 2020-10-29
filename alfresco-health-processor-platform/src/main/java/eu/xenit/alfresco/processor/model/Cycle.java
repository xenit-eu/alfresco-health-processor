package eu.xenit.alfresco.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Information object containing the parameters for the tracking activity.
 */
@Data
@AllArgsConstructor
public class Cycle {
    private int txnLimit;
    private long firstTxn;
    private int timeIncrementSeconds;
    private long firstCommitTime;

    private long transactionId;
    private long commitTimeMs;
}
