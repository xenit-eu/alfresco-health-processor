package eu.xenit.alfresco.healthprocessor.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import org.alfresco.service.cmr.repository.NodeRef;

public class AssertHealthProcessorPlugin implements HealthProcessorPlugin {
    private boolean enabled;

    public AssertHealthProcessorPlugin() {
        this(true);
    }

    public AssertHealthProcessorPlugin(boolean enabled) {
        this.enabled = enabled;
    }

    private final Queue<Set<NodeRef>> invocations = new LinkedList<>();

    @Override
    public Set<NodeHealthReport> process(Set<NodeRef> nodeRefs) {
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
