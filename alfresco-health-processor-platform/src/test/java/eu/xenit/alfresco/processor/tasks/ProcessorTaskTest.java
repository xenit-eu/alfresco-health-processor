package eu.xenit.alfresco.processor.tasks;

import eu.xenit.alfresco.processor.service.ProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
public class ProcessorTaskTest {
    @Mock
    private ProcessorService processorService;

    @Test
    public void invokeProcessorServiceTest() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        doAnswer((Answer<Void>) invocation -> {
            executed.set(true);
            return null;
        }).when(processorService).validateHealth();
        new ProcessorTask(processorService)
                .execute();
        assertTrue(executed.get());
    }
}
