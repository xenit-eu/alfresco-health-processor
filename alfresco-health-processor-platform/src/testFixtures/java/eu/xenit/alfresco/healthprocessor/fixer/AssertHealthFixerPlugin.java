package eu.xenit.alfresco.healthprocessor.fixer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public class AssertHealthFixerPlugin implements HealthFixerPlugin {

    private final boolean enabled;

    private final Queue<Set<NodeHealthReport>> invocations = new LinkedList<>();

    public AssertHealthFixerPlugin() {
        this(true);
    }

    public AssertHealthFixerPlugin(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Nonnull
    @Override
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass,
            Set<NodeHealthReport> unhealthyReports) {
        invocations.offer(unhealthyReports);
        return Collections.emptySet();
    }

    public int getNumberOfInvocations() {
        return invocations.size();
    }

    public void expectNoInvocation() {
        assertThat(invocations.peek(), nullValue());
    }

    public void expectInvocation(NodeHealthReport... expectedRefs) {
        expectInvokedAndConsume(set -> assertThat(set, containsInAnyOrder(expectedRefs)));
    }

    public void expectInvokedAndConsume(Consumer<Set<NodeHealthReport>> consumer) {
        assertThat(invocations.peek(), is(notNullValue()));
        consumer.accept(invocations.poll());
    }
}
