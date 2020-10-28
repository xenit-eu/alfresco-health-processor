package eu.xenit.alfresco.processor.service;

public interface HealthProcessorConfiguration {
    boolean isEnabled();
    boolean isRunOnce();
    ProcessorService.Scope getScope();
    int getTransactionLimit();
    long getFirstTransaction();
    int getTimeIncrementSeconds();
    long getFirstCommitTime();
    String getFirstCommitTimeValue();
}
