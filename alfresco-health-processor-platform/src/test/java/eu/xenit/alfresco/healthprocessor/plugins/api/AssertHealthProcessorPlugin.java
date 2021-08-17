package eu.xenit.alfresco.healthprocessor.plugins.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;

@Slf4j
public class AssertHealthProcessorPlugin extends ToggleableHealthProcessorPlugin {

    private final Queue<Set<NodeRef>> invocations = new LinkedBlockingQueue<>();

    public AssertHealthProcessorPlugin() {
        setEnabled(true);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Set<NodeHealthReport> doProcess(Set<NodeRef> nodeRefs) {
        invocations.offer(nodeRefs);
        return Collections.emptySet();
    }

    public int getNumberOfInvocations() {
        return invocations.size();
    }

    public void expectNoInvocation() {
        assertThat(invocations.peek(), nullValue());
    }

    public void expectInvocation(NodeRef... expectedRefs) {
        expectInvokedAndConsume(set -> assertThat(set, containsInAnyOrder(expectedRefs)));
    }

    public void expectInvokedAndConsume(Consumer<Set<NodeRef>> consumer) {
        assertThat(invocations.peek(), is(notNullValue()));
        consumer.accept(invocations.poll());
    }
}
