package eu.xenit.alfresco.healthprocessor.indexing.threshold;

import lombok.NonNull;
import org.alfresco.repo.domain.node.StoreEntity;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.solr.Transaction;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.alfresco.repo.domain.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ThresholdIndexingStrategyTransactionIdMergerTest {

    private static final @NonNull Random RANDOM = new Random();
    private static final @NonNull List<StoreRef> WORKSPACE_AND_ARCHIVE_STORE_REFS = List.of(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
    private static final @NonNull List<StoreRef> FILTERED_OUT_STORE_REFS = List.of(new StoreRef("we", "are"),
            new StoreRef("really", "not"), new StoreRef("supposed", "to"), new StoreRef("be", "here"));
    private static final int THRESHOLD = 5;
    private static final int TRANSACTIONS_BATCH_SIZE = 2;

    // * 2: for each workspace / archive document, we're going to add a node from a store that should be filtered out.
    private final @NonNull ArrayList<Node> nodes = new ArrayList<>(THRESHOLD * 2);
    private final @NonNull ThresholdIndexingStrategyTransactionIdFetcher fetcher = mock(ThresholdIndexingStrategyTransactionIdFetcher.class);
    private final @NonNull SearchTrackingComponent searchTrackingComponent = mock(SearchTrackingComponent.class);
    private final @NonNull AtomicInteger transactionIndexCounter = new AtomicInteger(0);
    private final @NonNull BlockingDeque<Set<NodeRef>> queuedNodes = new LinkedBlockingDeque<>();
    private final @NonNull ThresholdIndexingStrategyTransactionIdMerger merger;
    private final @NonNull ThresholdIndexingStrategyConfiguration configuration = new ThresholdIndexingStrategyConfiguration(-1, -1, THRESHOLD, -1, -1);

    public ThresholdIndexingStrategyTransactionIdMergerTest() throws InterruptedException {
        for (int i = 0; i < THRESHOLD * 2; i ++) {
            StoreRef storeRef = (i % 2 == 0)? WORKSPACE_AND_ARCHIVE_STORE_REFS.get(RANDOM.nextInt(WORKSPACE_AND_ARCHIVE_STORE_REFS.size())) :
                    FILTERED_OUT_STORE_REFS.get(RANDOM.nextInt(FILTERED_OUT_STORE_REFS.size()));
            // +1: cf. .getNodes() mock.
            nodes.add(createDummyNode(storeRef, i + 1L, Integer.toString(i)));
        }

        when(fetcher.getNextTransactions()).thenAnswer(invocation -> {
           int transactionIndex = transactionIndexCounter.getAndIncrement();
           return IntStream.range(TRANSACTIONS_BATCH_SIZE * transactionIndex, Math.min(TRANSACTIONS_BATCH_SIZE * (transactionIndex + 1), nodes.size() + 1)) // + 1: cf. .getNodes() mock.
                   .mapToObj(index -> {
                       Transaction transaction = mock(Transaction.class);
                       when(transaction.getId()).thenReturn((long) index);
                      return transaction;
                   }).collect(Collectors.toList());
        });

        doAnswer(invocation -> {
            NodeParameters nodeParameters = invocation.getArgument(0);
            SearchTrackingComponent.NodeQueryCallback nodeHandler = invocation.getArgument(1);

            // 0 is a special transaction.
            // Transaction 0 is so big that it should be filtered out.
            nodeParameters.getTransactionIds().forEach(transactionId -> {
                if (transactionId == 0) {
                    for (int i = 0; i < THRESHOLD; i ++) nodeHandler.handleNode(createDummyNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 0, Integer.toString(i)));
                } else nodeHandler.handleNode(nodes.get(transactionId.intValue() - 1));
            });

            return null;
        }).when(searchTrackingComponent).getNodes(any(), any());

        merger = new ThresholdIndexingStrategyTransactionIdMerger(fetcher, queuedNodes, configuration,
                searchTrackingComponent);
    }

    @BeforeEach
    void setUp() {
        transactionIndexCounter.set(0);
        queuedNodes.clear();
    }

    @Test
    void run() throws InterruptedException {
        Thread thread = new Thread(merger);
        thread.start();

        try {
            // We just queued THRESHOLD * 2 transactions, of which half should be filtered.
            // As a result, we should expect exactly one transaction here.
            Set<NodeRef> queuedNodeRefs = queuedNodes.takeFirst();
            assertEquals(nodes.stream()
                    .filter(node -> WORKSPACE_AND_ARCHIVE_STORE_REFS.contains(node.getStore().getStoreRef()))
                    .map(Node::getNodeRef)
                    .collect(Collectors.toSet()), queuedNodeRefs);
            // Exactly one transaction, so the next poll should contain the end signal of the worker.
            queuedNodeRefs = queuedNodes.takeFirst();
            assertNotNull(queuedNodeRefs);
            assertTrue(queuedNodeRefs.isEmpty());

            thread.join(3_000);
            assertFalse(thread.isAlive());
        } finally {
            thread.interrupt();
        }
    }

    private static @NonNull Node createDummyNode(@NonNull StoreRef storeRef, long transactionId, @NonNull String nodeUUID) {
        StoreEntity storeEntity = mock(StoreEntity.class);
        when(storeEntity.getStoreRef()).thenReturn(storeRef);
        Node dummyNode = mock(Node.class);
        when(dummyNode.getStore()).thenReturn(storeEntity);
        when(dummyNode.getNodeRef()).thenReturn(new NodeRef(storeRef, nodeUUID));
        when(dummyNode.getTransaction()).thenReturn(mock(TransactionEntity.class));
        when(dummyNode.getTransaction().getId()).thenReturn(transactionId);
        return dummyNode;
    }

}