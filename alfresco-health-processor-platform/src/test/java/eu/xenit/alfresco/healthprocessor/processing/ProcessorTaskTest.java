package eu.xenit.alfresco.healthprocessor.processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessorTaskTest {

    @Mock
    private ProcessorService processorService;
    @Mock
    private JobLockService jobLockService;
    private AssertTransactionHelper transactionHelper;

    @BeforeEach
    void setup() {
        transactionHelper = new AssertTransactionHelper();
    }

    @Test
    void startTask_multiTenant() {
        ProcessorTask task = task(ProcConfigUtil.config(false));

        task.startIfNotRunningAsUser();

        verify(processorService).execute();
        verifyNoInteractions(jobLockService);
    }

    @Test
    void startTask_singleTenant() {
        ProcessorTask task = task(ProcConfigUtil.config(true));

        task.startIfNotRunningAsUser();

        verify(processorService).execute();
        verify(jobLockService).getLock(eq(ProcessorTask.LOCK_QNAME), eq(ProcessorTask.LOCK_TTL), any());
    }

    @Test
    void startTask_singleTenant_lockClaimedByOtherNode() {
        when(jobLockService.getLock(eq(ProcessorTask.LOCK_QNAME), eq(ProcessorTask.LOCK_TTL), any()))
                .thenThrow(new LockAcquisitionException(ProcessorTask.LOCK_QNAME, "lock-123-token"));

        ProcessorTask task = task(ProcConfigUtil.config(true));

        task.startIfNotRunningAsUser();

        verify(processorService, never()).execute();
        verify(jobLockService).getLock(eq(ProcessorTask.LOCK_QNAME), eq(ProcessorTask.LOCK_TTL), any());
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
        return new ProcessorTask(configuration, processorService, transactionHelper, jobLockService);
    }

}