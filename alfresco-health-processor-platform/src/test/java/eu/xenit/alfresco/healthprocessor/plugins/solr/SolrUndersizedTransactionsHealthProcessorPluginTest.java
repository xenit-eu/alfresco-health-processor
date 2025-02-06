package eu.xenit.alfresco.healthprocessor.plugins.solr;

import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.NonNull;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SolrUndersizedTransactionsHealthProcessorPluginTest {

    private static final int AMOUNT_OF_TEST_NODE_REFS = 100;
    private static final @NonNull Random RANDOM = new Random();

    private final @NonNull Properties properties = new Properties();
    private final @NonNull HashMap<NodeRef, Long> testNodes = new HashMap<>(AMOUNT_OF_TEST_NODE_REFS);
    private final @NonNull HashSet<Long> touchedNodeIDs;

    private TransactionHelper transactionHelper;
    private AbstractNodeDAOImpl nodeDAO;
    private SolrUndersizedTransactionsHealthProcessorPlugin plugin;

    public SolrUndersizedTransactionsHealthProcessorPluginTest() {
        for (int i = 0; i < AMOUNT_OF_TEST_NODE_REFS; i++) {
            NodeRef nodeRef = new NodeRef("workspace://SpacesStore/" + UUID.randomUUID());
            testNodes.put(nodeRef, (long) i);
        }

        touchedNodeIDs = new HashSet<>(AMOUNT_OF_TEST_NODE_REFS);

        properties.put("eu.xenit.alfresco.healthprocessor.indexing.strategy", "threshold");
    }

    @BeforeEach
    void setUp() {
        this.transactionHelper = mock(TransactionHelper.class);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionHelper).inNewTransaction(any(Runnable.class), eq(false));

        this.nodeDAO = mock(AbstractNodeDAOImpl.class);
        when(nodeDAO.getNodePair(any(NodeRef.class))).thenAnswer(invocation -> {
            NodeRef nodeRef = invocation.getArgument(0);
            return new Pair<>(testNodes.get(nodeRef), nodeRef);
        });
        when(nodeDAO.getCurrentTransactionId(eq(true))).thenReturn(RANDOM.nextLong());
        when(nodeDAO.touchNodes(anyLong(), anyList())).thenAnswer(invocation -> {
            List<Long> nodeIDs = invocation.getArgument(1);
            touchedNodeIDs.addAll(nodeIDs);
            return null;
        });

        touchedNodeIDs.clear();

        plugin = new SolrUndersizedTransactionsHealthProcessorPlugin(true, 1, properties, transactionHelper, nodeDAO);
    }

    @Test
    void doProcess() throws InterruptedException {
        plugin.process(testNodes.keySet());
        Thread.sleep(2_000);
        verify(nodeDAO, times(testNodes.size())).getNodePair(any(NodeRef.class));
        verify(transactionHelper, times(1)).inNewTransaction(any(Runnable.class), eq(false));
        verify(nodeDAO, times(1)).touchNodes(anyLong(), anyList());
        assertEquals(new HashSet<>(testNodes.values()), touchedNodeIDs);
    }

    @Test
    void testChosenIndexerStrategy() {
        Properties wrongProperties = new Properties();
        wrongProperties.put("eu.xenit.alfresco.healthprocessor.indexing.strategy", "wrong-strategy");
        assertThrows(IllegalStateException.class, () -> new SolrUndersizedTransactionsHealthProcessorPlugin(true, 1, wrongProperties, transactionHelper, nodeDAO));
    }

}