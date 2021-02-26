package eu.xenit.alfresco.healthprocessor.plugins;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NoOpHealthProcessorPluginTest {

    @Test
    void returnsStatusNone() {
        NoOpHealthProcessorPlugin plugin = new NoOpHealthProcessorPlugin();

        Set<NodeHealthReport> ret = plugin.process(set(TestNodeRefs.REFS[0], TestNodeRefs.REFS[1]));

        assertThat(ret, hasSize(2));
        ret.stream().map(NodeHealthReport::getStatus).forEach(s -> assertThat(s, is(equalTo(NodeHealthStatus.NONE))));
    }

}