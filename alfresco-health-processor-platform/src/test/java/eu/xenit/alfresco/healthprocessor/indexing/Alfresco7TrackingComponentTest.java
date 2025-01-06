package eu.xenit.alfresco.healthprocessor.indexing;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent.NodeInfo;
import java.util.Collections;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.search.SearchTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Alfresco7TrackingComponentTest {

    @Mock
    private SearchTrackingComponent searchTrackingComponent;

    private TrackingComponent trackingComponent;

    @BeforeEach
    void setup() {
        trackingComponent = new Alfresco7TrackingComponent(searchTrackingComponent);
    }

    @Test
    void getMaxTxnId() {
        when(searchTrackingComponent.getMaxTxnId()).thenReturn(101L);

        assertThat(trackingComponent.getMaxTxnId(), is(equalTo(101L)));
    }

    @Test
    void getNodesForTxnIds() {
        doAnswer(invocation -> {
            NodeQueryCallback cb = invocation.getArgument(1);

            cb.handleNode(nodeEntity(1L, 101L, "abc-123"));
            cb.handleNode(nodeEntity(1L, 102L, "xyz-987"));

            return null;
        }).when(searchTrackingComponent).getNodes(any(), any());

        assertThat(trackingComponent.getNodesForTxnIds(Collections.singletonList(1L)),
                containsInAnyOrder(
                        new NodeInfo(1L, 101L, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "abc-123")),
                        new NodeInfo(1L, 102L, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "xyz-987"))));
    }

    @Test
    void getNodesForEmptyTxnIdList() {
        trackingComponent.getNodesForTxnIds(Collections.emptyList());
        verify(searchTrackingComponent, never()).getNodes(any(), any());
    }

    private Node nodeEntity(long txnId, long nodeId, String uuid) {
        Node ret = mock(Node.class);
        TransactionEntity transactionEntity = mock(TransactionEntity.class);
        when(transactionEntity.getId()).thenReturn(txnId);
        when(ret.getTransaction()).thenReturn(transactionEntity);
        when(ret.getId()).thenReturn(nodeId);
        when(ret.getNodeRef()).thenReturn(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, uuid));
        return ret;
    }


}
