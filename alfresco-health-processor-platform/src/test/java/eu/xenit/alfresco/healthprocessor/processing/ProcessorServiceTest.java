package eu.xenit.alfresco.healthprocessor.processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import java.util.Collections;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ProcessorServiceTest {

    private RetryingTransactionHelper retryingTransactionHelper;
    @Mock
    private IndexingStrategy indexingStrategy;

    @BeforeEach
    void setup() {
        retryingTransactionHelper = mock(RetryingTransactionHelper.class);
        Answer<?> answer = invocation -> invocation.getArgument(0, RetryingTransactionCallback.class).execute();
        lenient().when(retryingTransactionHelper.doInTransaction(any())).thenAnswer(answer);
        lenient().when(retryingTransactionHelper.doInTransaction(any(), anyBoolean())).thenAnswer(answer);
        lenient().when(retryingTransactionHelper.doInTransaction(any(), anyBoolean(), anyBoolean())).thenAnswer(answer);
    }

    @Test
    void execute_resetBeforeFetchNodes() {
        ProcessorService service = service();

        when(indexingStrategy.getNextNodeIds(anyInt())).thenReturn(Collections.emptySet());

        service.execute();

        verify(indexingStrategy).reset();
        verify(indexingStrategy).getNextNodeIds(anyInt());
    }

    private ProcessorService service() {
        return service(ProcConfigUtil.defaultConfig());
    }

    private ProcessorService service(ProcessorConfiguration config) {
        return new ProcessorService(config, indexingStrategy, retryingTransactionHelper);
    }

}