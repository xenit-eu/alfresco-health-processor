package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import org.alfresco.util.Pair;


public class AssertTransactionHelper implements TransactionHelper {

    Queue<Pair<Boolean, Boolean>> invocations = new LinkedBlockingQueue<>();

    @Override
    public <T> T inTransaction(Supplier<T> supplier, boolean readOnly, boolean requiresNew) {
        invocations.add(new Pair<>(readOnly, requiresNew));
        return supplier.get();
    }

    public void expectInvocation(boolean readOnlyExpected, boolean requiresNewExpected) {
        Pair<Boolean, Boolean> invoc = invocations.poll();
        assertThat(invoc, is(notNullValue()));
        assertThat(invoc.getFirst(), is(equalTo(readOnlyExpected)));
        assertThat(invoc.getSecond(), is(equalTo(requiresNewExpected)));
    }

    public void expectInvocation() {
        assertThat(invocations.poll(), is(not(nullValue())));
    }
}
