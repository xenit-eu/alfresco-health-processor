package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.alfresco.repo.domain.node.AuditablePropertiesEntity;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.NodeVersionKey;
import org.alfresco.repo.domain.node.StoreEntity;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.solr.SOLRTrackingComponent.NodeQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlfrescoTrackingComponentTest {

    @Mock
    private SOLRTrackingComponent solrTrackingComponent;

    private TrackingComponent trackingComponent;

    @BeforeEach
    void setup() {
        trackingComponent = new AlfrescoTrackingComponent(solrTrackingComponent);
    }

    @Test
    void getMaxTxnId() {
        when(solrTrackingComponent.getMaxTxnId()).thenReturn(101L);

        assertThat(trackingComponent.getMaxTxnId(), is(equalTo(101L)));
    }

    @Test
    void getNodesForTxnIds() {
        doAnswer(invocation -> {
            NodeQueryCallback cb = invocation.getArgument(1);

            cb.handleNode(nodeEntity("abc-123"));
            cb.handleNode(nodeEntity("xyz-987"));

            return null;
        }).when(solrTrackingComponent).getNodes(any(), any());

        assertThat(trackingComponent.getNodesForTxnIds(Collections.singletonList(1L)),
                containsInAnyOrder(
                        new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "abc-123"),
                        new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "xyz-987")));
    }

    private Node nodeEntity(String uuid) {
        return new Node() {
            @Override
            public NodeVersionKey getNodeVersionKey() {
                return null;
            }

            @Override
            public void lock() {

            }

            @Override
            public NodeRef getNodeRef() {
                return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, uuid);
            }

            @Override
            public Status getNodeStatus(QNameDAO qnameDAO) {
                return null;
            }

            @Override
            public Pair<Long, NodeRef> getNodePair() {
                return null;
            }

            @Override
            public boolean getDeleted(QNameDAO qnameDAO) {
                return false;
            }

            @Override
            public Long getVersion() {
                return null;
            }

            @Override
            public StoreEntity getStore() {
                return null;
            }

            @Override
            public String getUuid() {
                return null;
            }

            @Override
            public Long getTypeQNameId() {
                return null;
            }

            @Override
            public Long getLocaleId() {
                return null;
            }

            @Override
            public TransactionEntity getTransaction() {
                return null;
            }

            @Override
            public AuditablePropertiesEntity getAuditableProperties() {
                return null;
            }

            @Override
            public String getShardKey() {
                return null;
            }

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public Long getAclId() {
                return null;
            }
        };
    }


}