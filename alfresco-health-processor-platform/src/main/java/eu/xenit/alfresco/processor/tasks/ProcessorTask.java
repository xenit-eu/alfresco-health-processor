package eu.xenit.alfresco.processor.tasks;

import eu.xenit.alfresco.processor.service.IProcessorService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class ProcessorTask {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorTask.class);

    private final IProcessorService processorService;

    public void execute() {
        logger.trace("Invoke health-processor service here");
        processorService.validateHealth();
        logger.trace("Invoking health-processor service completed");
    }
}
