package eu.xenit.alfresco.healthprocessor.processing;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessorTaskTest {

    @Mock
    private ProcessorService processorService;

    @Test
    void startIfNotRunning() {
        ProcessorTask task = task();

        task.startIfNotRunningAsUser();

        verify(processorService).execute();
    }

    private ProcessorTask task() {
        return task(ProcConfigUtil.defaultConfig());
    }

    private ProcessorTask task(ProcessorConfiguration configuration) {
        return new ProcessorTask(configuration, processorService);
    }

}