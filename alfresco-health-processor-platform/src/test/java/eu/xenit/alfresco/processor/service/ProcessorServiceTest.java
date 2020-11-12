package eu.xenit.alfresco.processor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProcessorServiceTest {
    @Spy
    private ProcessorService processorService;
    @Mock
    private ExecutorService executorService;
    @Mock
    private HealthProcessorConfiguration configurationService;

    @Test
    public void validateHealthWhenDisabledTest() {
        when(configurationService.isEnabled())
                .thenAnswer(i -> false);
        processorService.setConfiguration(configurationService);
        processorService.validateHealth();
        verify(executorService, never())
                .submit(any(Runnable.class));
    }

    @Test
    public void validateHealthWhenEnabledTest() {
        when(configurationService.isEnabled())
                .thenAnswer(i -> true);
        final AtomicBoolean executed = new AtomicBoolean(false);
        doAnswer(i -> {
            executed.set(true);
            return null;
        }).when(executorService).submit(any(Runnable.class));
        processorService.setConfiguration(configurationService);
        processorService.setExecutorService(executorService);
        processorService.validateHealth();
        assertTrue(executed.get());
    }
}
