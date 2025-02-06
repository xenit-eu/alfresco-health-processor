package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.StoreEntity;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
class ThresholdIndexingStrategyTest {

    // The queue mechanism is not fair / FIFO on purpose; however, that means that testing the entire indexing strategy
    // with more than one worker is not possible. Workers fetching some transactions faster than others is a possibility &
    // will result in totally different test outcomes.
    private final static int AMOUNT_OF_BACKGROUND_WORKERS = 1;
    private final static int TRANSACTION_BATCHES = 1;
    private final static int NODES_PER_TRANSACTION = 2;
    private final static int THRESHOLD = 10;
    private final static int AMOUNT_OF_TRANSACTIONS = AMOUNT_OF_BACKGROUND_WORKERS * (THRESHOLD / NODES_PER_TRANSACTION);
    private final static @NonNull ThresholdIndexingStrategyConfiguration CONFIGURATION
            = new ThresholdIndexingStrategyConfiguration(AMOUNT_OF_BACKGROUND_WORKERS, TRANSACTION_BATCHES, THRESHOLD,
            0, AMOUNT_OF_TRANSACTIONS);

    private final @NonNull AbstractNodeDAOImpl nodeDAO;
    private final @NonNull SearchTrackingComponent searchTrackingComponent;
    private final @NonNull ArrayList<Node> nodes = new ArrayList<>();
    private final @NonNull ThresholdIndexingStrategy indexingStrategy;

    public ThresholdIndexingStrategyTest() {
        for (int i = 0; i < AMOUNT_OF_TRANSACTIONS * NODES_PER_TRANSACTION; i ++) {
            Node node = mock(Node.class);
            when(node.getStore()).thenReturn(mock(StoreEntity.class));
            when(node.getStore().getStoreRef()).thenReturn(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            when(node.getNodeRef()).thenReturn(new NodeRef("workspace", "SpacesStore", String.valueOf(i)));
            when(node.getTransaction()).thenReturn(mock(TransactionEntity.class));
            when(node.getTransaction().getId()).thenReturn((long) Math.floorDiv(i, NODES_PER_TRANSACTION)); // TODO: might be wrong.
            nodes.add(node);
        }

        this.nodeDAO = mock(AbstractNodeDAOImpl.class);
        when(nodeDAO.getMinTxnId()).thenReturn(0L);

        this.searchTrackingComponent = mock(SearchTrackingComponent.class);
        when(searchTrackingComponent.getMaxTxnId()).thenReturn((long) AMOUNT_OF_TRANSACTIONS);
        when(searchTrackingComponent.getTransactions(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenAnswer(invocation -> {
            long start = invocation.getArgument(0);
            long end = Math.min(invocation.getArgument(2), Math.min(start + (int) invocation.getArgument(4),
                    AMOUNT_OF_TRANSACTIONS));

            return LongStream.range(start, end)
                    .mapToObj(txnId -> {
                        Transaction transaction = mock(Transaction.class);
                        when(transaction.getId()).thenReturn(txnId);
                        return transaction;
                    }).collect(Collectors.toList());
        });
        doAnswer(invocation -> {
            NodeParameters nodeParameters = invocation.getArgument(0);
            SearchTrackingComponent.NodeQueryCallback callback = invocation.getArgument(1);

            nodeParameters.getTransactionIds()
                    .stream()
                    .flatMap(index -> nodes.stream().filter(node -> Objects.equals(node.getTransaction().getId(), index)))
                    .forEach(callback::handleNode);
            return null;
        }).when(searchTrackingComponent).getNodes(any(), any());

        this.indexingStrategy = new ThresholdIndexingStrategy(CONFIGURATION, nodeDAO, searchTrackingComponent);
    }

    @Test
    void getNextNodeIds() {
        HashSet<NodeRef> allNodeRefsToExpect = nodes.stream().map(Node::getNodeRef).collect(Collectors.toCollection(HashSet::new));
        log.info("Expecting {} node refs.", allNodeRefsToExpect.size());

        indexingStrategy.onStart();
        try {
            for (int i = 0; i < AMOUNT_OF_BACKGROUND_WORKERS; i++) {
                Set<NodeRef> nodeRefs = indexingStrategy.getNextNodeIds(Integer.MAX_VALUE); // The 'amount' is ignored anyway.
                assertEquals(THRESHOLD, nodeRefs.size());
                assertTrue(allNodeRefsToExpect.containsAll(nodeRefs));
                allNodeRefsToExpect.removeAll(nodeRefs);
            }
            assertTrue(allNodeRefsToExpect.isEmpty());
            assertTrue(indexingStrategy.getNextNodeIds(1).isEmpty()); // End signal.
        } finally {
            indexingStrategy.onStop();
        }
    }

    @Test
    public void testArguments() {
        ThresholdIndexingStrategyConfiguration configuration = new ThresholdIndexingStrategyConfiguration(0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ThresholdIndexingStrategy(configuration, nodeDAO, searchTrackingComponent));
    }

}