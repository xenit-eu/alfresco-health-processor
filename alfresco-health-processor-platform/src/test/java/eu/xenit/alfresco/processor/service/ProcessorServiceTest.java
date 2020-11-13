package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
        ProcessorService processorService = createNewProcessorService(
                executorService,
                configurationService);
        processorService.validateHealth();
        verify(executorService, never())
                .submit(any(Runnable.class));
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
        ProcessorService processorService = createNewProcessorService(
                executorService,
                configurationService);
        processorService.validateHealth();
        assertTrue(executed.get());
    }

    private ProcessorService createNewProcessorService(
            ExecutorService executorService,
            HealthProcessorConfiguration configuration) {
        return new ProcessorService(
                null,
                executorService,
                configuration
        );
    }
}
