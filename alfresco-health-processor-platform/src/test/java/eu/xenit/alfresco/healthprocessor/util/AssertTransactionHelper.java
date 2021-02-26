package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;


public class AssertTransactionHelper implements TransactionHelper {

    Queue<Boolean> invocations = new LinkedBlockingQueue<>();

    @Override
    public <T> T inNewTransaction(Supplier<T> supplier, boolean readOnly) {
        invocations.add(readOnly);
        return supplier.get();
    }

    public void expectInvocation(boolean readOnlyExpected) {
        assertThat(invocations.poll(), is(equalTo(readOnlyExpected)));
    }

    public void expectInvocation() {
        assertThat(invocations.poll(), is(not(nullValue())));
    }
}
