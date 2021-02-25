package eu.xenit.alfresco.healthprocessor.processing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsibilities: upon trigger ({@link #startIfNotRunning()}) decide if the processor should be triggered, trigger
 * processor, persist state so other Alfresco nodes can decide if the processor should be triggered.
 */
@AllArgsConstructor
@Slf4j
public class ProcessorTask {

    private final ProcessorConfiguration configuration;
    private final ProcessorService processorService;
//    private final ProcessorAttributeService processorAttributeService;

    public void startIfNotRunning() {
        if (configuration.isSingleTenant() && isAlreadyRunningOnAnyTenant()) {
            log.info("Processor triggered but process is already running...");
            return;
        }

        try {
            // TODO persist multi-tenant-wide state wide state with attributeService
            start();
        } catch (Exception e) {
            // TODO cleanup attributes
        }
    }

    private boolean isAlreadyRunningOnAnyTenant() {
        return false; // TODO fetch multi-tenant-wide state with attributeService
    }

    private void start() {
        processorService.execute();
    }

}