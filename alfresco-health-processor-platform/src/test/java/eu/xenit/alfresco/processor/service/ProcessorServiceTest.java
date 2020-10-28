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
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class ProcessorServiceTest {
    @Mock
    private ExecutorService executorService;
    @Spy
    private ProcessorServiceImpl processorService;

    @Test
    public void validateHealthTest() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        doAnswer(i -> {
            Runnable submittedAction = i.getArgument(0);
            submittedAction.run();
            executed.set(true);
            return null;
        }).when(executorService).submit(any(Runnable.class));
        doNothing()
                .when(processorService)
                .doInTransaction(any(Runnable.class), anyBoolean(), anyBoolean());
        processorService.setExecutorService(executorService);
        processorService.validateHealth();
        assertTrue(executed.get());
    }
}
