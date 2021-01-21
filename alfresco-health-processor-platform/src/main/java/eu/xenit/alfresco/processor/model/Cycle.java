package eu.xenit.alfresco.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Information object containing the parameters for the tracking activity.
 */
@Data
@AllArgsConstructor
public class Cycle {
    /**
     * Transaction ID to start scanning from
      */
    private long firstTxn;
    /**
     * Housekeeping: keep max txn ID to know when to stop
     * TODO replace with Max Txn from Solr
     */
    private long maxTxn;
    /**
     * Number of transactions to process at once
     */
    private long txnBatchSize;
    /**
     * Housekeeping: the ID of the current transaction being processed
     */
    private long currentTransactionId;
    /**
     * Time window between transactions in seconds
     */
    private long timeIncrementSeconds;
    /**
     * Commit Time to start scanning from
     */
    private long firstCommitTime;
    /**
     * Housekeeping: the commit time of the current transaction being processed
     */
    private long currentCommitTimeMs;
}
