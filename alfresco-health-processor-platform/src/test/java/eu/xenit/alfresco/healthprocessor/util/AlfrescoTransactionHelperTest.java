package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class AlfrescoTransactionHelperTest {

    private RetryingTransactionHelper retryingTransactionHelper;
    private TransactionHelper transactionHelper;

    @BeforeEach
    void setup() {
        retryingTransactionHelper = mock(RetryingTransactionHelper.class);
        Answer<?> answer = invocation -> invocation.getArgument(0, RetryingTransactionCallback.class).execute();
        lenient().when(retryingTransactionHelper.doInTransaction(any(), anyBoolean(), anyBoolean())).thenAnswer(answer);

        transactionHelper = new AlfrescoTransactionHelper(retryingTransactionHelper);
    }

    @Test
    void inNewTransaction_assertRequiresNew() {
        transactionHelper.inNewTransaction(() -> assertThat(2, is(equalTo(1 + 1))), true);
        verify(retryingTransactionHelper).doInTransaction(any(), eq(true), eq(true));
        verifyNoMoreInteractions(retryingTransactionHelper);
        reset(retryingTransactionHelper);

        transactionHelper.inNewTransaction(() -> assertThat(2, is(equalTo(1 + 1))), false);
        verify(retryingTransactionHelper).doInTransaction(any(), eq(false), eq(true));
        verifyNoMoreInteractions(retryingTransactionHelper);
    }

    @Test
    void inTransaction_assertDoesNotRequireNew() {
        transactionHelper.inTransaction(() -> assertThat(2, is(equalTo(1 + 1))), true);
        verify(retryingTransactionHelper).doInTransaction(any(), eq(true), eq(false));
        verifyNoMoreInteractions(retryingTransactionHelper);
        reset(retryingTransactionHelper);

        transactionHelper.inTransaction(() -> assertThat(2, is(equalTo(1 + 1))), false);
        verify(retryingTransactionHelper).doInTransaction(any(), eq(false), eq(false));
        verifyNoMoreInteractions(retryingTransactionHelper);
    }

}