package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.alfresco.service.cmr.repository.NodeRef;

public class AssertIndexingStrategy implements IndexingStrategy {

    private final Queue<NodeRef> nodeQueue = new LinkedBlockingQueue<>();
    private int numberOfResetInvocations;
    private int numberOfGetNextNodeIdsInvocations;

    @Override
    public void reset() {
        numberOfResetInvocations++;
    }

    @Override
    public Set<NodeRef> getNextNodeIds(int amount) {
        numberOfGetNextNodeIdsInvocations++;
        Set<NodeRef> ret = new HashSet<>();

        for (int i = 0; i < amount; i++) {
            if (nodeQueue.peek() != null) {
                ret.add(nodeQueue.poll());
            }
        }

        return ret;
    }

    public void nextAnswer(NodeRef... nodes) {
        this.nextAnswer(Arrays.asList(nodes));
    }

    public void nextAnswer(List<NodeRef> nodes) {
        nodeQueue.addAll(nodes);
    }

    public void expectResetInvocations(int amount) {
        assertThat(numberOfResetInvocations, is(equalTo(amount)));
    }

    public void expectGetNextNodeIdsInvocations(int amount) {
        assertThat(numberOfGetNextNodeIdsInvocations, is(equalTo(amount)));

    }
}
