package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessorServiceTest {
    @Test
    public void validateHealthWhenDisabledTest() {
        HealthProcessorConfiguration configurationService =
                Mockito.mock(HealthProcessorConfiguration.class);
        ExecutorService executorService =
                Mockito.mock(ExecutorService.class);
        when(configurationService.isEnabled())
                .thenAnswer(i -> false);
        ProcessorAttributeService processorAttributeService =
                Mockito.mock(ProcessorAttributeService.class);
        ProcessorService processorService = createNewProcessorService(
                executorService,
                configurationService,
                processorAttributeService);
        processorService.validateHealth();
        verify(processorAttributeService, never())
                .getAttribute(anyString(), anyBoolean());
    }

    @Test
    public void validateHealthWhenEnabledTest() {
        HealthProcessorConfiguration configurationService =
                Mockito.mock(HealthProcessorConfiguration.class);
        when(configurationService.isEnabled())
                .thenAnswer(i -> true);

        final AtomicBoolean executed = new AtomicBoolean(false);
        ExecutorService executorService =
                Mockito.mock(ExecutorService.class);
        doAnswer(i -> {
            executed.set(true);
            return null;
        }).when(executorService).submit(any(Runnable.class));
        ProcessorAttributeService processorAttributeService =
                Mockito.mock(ProcessorAttributeService.class);
        doReturn(false)
                .when(processorAttributeService)
                .getAttribute(anyString(), anyBoolean());
        ProcessorService processorService = createNewProcessorService(
                executorService,
                configurationService,
                processorAttributeService);
        processorService.validateHealth();
        assertTrue(executed.get());
    }

    private ProcessorService createNewProcessorService(
            ExecutorService executorService,
            HealthProcessorConfiguration configuration,
            ProcessorAttributeService processorAttributeService) {
        return new ProcessorService(
                null,
                executorService,
                configuration,
                processorAttributeService
        );
    }
}
