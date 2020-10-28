package eu.xenit.alfresco.processor.tasks;

import eu.xenit.alfresco.processor.service.ProcessorService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessorTask {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorTask.class);

    @Getter @Setter
    protected ProcessorService processorService;

    protected ProcessorTask(){}
    public ProcessorTask(ProcessorService processorService) {
        this.processorService = processorService;
    }

    public void execute() {
        logger.trace("Invoke health-processor service here");
        processorService.validateHealth();
        logger.trace("Invoking health-processor service completed");
    }
}
