package eu.xenit.alfresco.healthprocessor.processing;

import static org.mockito.Mockito.verify;

import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessorTaskTest {

    @Mock
    private ProcessorService processorService;
    private AssertTransactionHelper transactionHelper;

    @BeforeEach
    void setup() {
        transactionHelper = new AssertTransactionHelper();
    }

    @Test
    void startIfNotRunning_invokesProcessorService() {
        ProcessorTask task = task();

        task.startIfNotRunningAsUser();

        verify(processorService).execute();
    }

    @Test
    void startIfNotRunning_requiresATransaction() {
        ProcessorTask task = task();

        task.startIfNotRunningAsUser();

        transactionHelper.expectInvocation(true, false);
    }

    private ProcessorTask task() {
        return task(ProcConfigUtil.defaultConfig());
    }

    private ProcessorTask task(ProcessorConfiguration configuration) {
        return new ProcessorTask(configuration, processorService, transactionHelper);
    }

}