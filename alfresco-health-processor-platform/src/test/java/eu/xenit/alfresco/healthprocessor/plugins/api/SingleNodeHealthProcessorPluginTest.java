package eu.xenit.alfresco.healthprocessor.plugins.api;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class SingleNodeHealthProcessorPluginTest {

    @Test
    void divideAndConquer() {
        TestPlugin plugin = new TestPlugin();

        plugin.answers.add(new NodeHealthReport(NodeHealthStatus.HEALTHY, TestNodeRefs.REFS[0]));
        plugin.answers.add(new NodeHealthReport(NodeHealthStatus.UNHEALTHY, TestNodeRefs.REFS[1]));
        plugin.answers.add(new NodeHealthReport(NodeHealthStatus.NONE, TestNodeRefs.REFS[2]));

        Set<NodeHealthReport> ret = plugin
                .process(set(TestNodeRefs.REFS[0], TestNodeRefs.REFS[1], TestNodeRefs.REFS[2]));

        assertThat(plugin.invocations,
                containsInAnyOrder(TestNodeRefs.REFS[0], TestNodeRefs.REFS[1], TestNodeRefs.REFS[2]));
        assertThat(ret, hasSize(3));
    }

    @Slf4j
    @Getter
    static class TestPlugin extends SingleNodeHealthProcessorPlugin {

        List<NodeRef> invocations = new ArrayList<>();
        Queue<NodeHealthReport> answers = new LinkedBlockingQueue<>();

        @Override
        protected Logger getLogger() {
            return log;
        }

        @Override
        protected NodeHealthReport process(NodeRef nodeRef) {
            invocations.add(nodeRef);
            return answers.poll();
        }
    }

}