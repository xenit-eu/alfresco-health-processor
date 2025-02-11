package eu.xenit.alfresco.healthprocessor.indexing.txnaggregation;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.StoreEntity;
import org.alfresco.repo.domain.node.TransactionEntity;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static eu.xenit.alfresco.healthprocessor.indexing.txnaggregation.TransactionAggregationIndexingStrategyTransactionIdFetcherTest.QUERY_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class TransactionAggregationIndexingStrategyTest {

    // The queue mechanism is not fair / FIFO on purpose; however, that means that testing the entire indexing strategy
    // with more than one worker is not possible. Workers fetching some transactions faster than others is a possibility &
    // will result in totally different test outcomes.
    private static final int AMOUNT_OF_BACKGROUND_WORKERS = 1;
    private static final int TRANSACTION_BATCHES = 1;
    private static final int NODES_PER_TRANSACTION = 2;
    private static final int THRESHOLD = 10;
    private static final int AMOUNT_OF_TRANSACTIONS = AMOUNT_OF_BACKGROUND_WORKERS * (THRESHOLD / NODES_PER_TRANSACTION);
    private static final @NonNull TransactionAggregationIndexingStrategyConfiguration CONFIGURATION
            = new TransactionAggregationIndexingStrategyConfiguration(AMOUNT_OF_BACKGROUND_WORKERS, TRANSACTION_BATCHES, THRESHOLD,
            0, AMOUNT_OF_TRANSACTIONS);

    private final @NonNull AbstractNodeDAOImpl nodeDAO = mock(AbstractNodeDAOImpl.class);
    private final @NonNull JdbcTemplate dummyJdbcTemplate = mock(JdbcTemplate.class);
    private final @NonNull SearchTrackingComponent searchTrackingComponent = mock(SearchTrackingComponent.class);
    private final @NonNull ArrayList<Node> nodes = new ArrayList<>();
    private final @NonNull TransactionAggregationIndexingStrategy indexingStrategy;

    public TransactionAggregationIndexingStrategyTest() {
        for (int i = 0; i < AMOUNT_OF_TRANSACTIONS * NODES_PER_TRANSACTION; i ++) {
            Node node = mock(Node.class);
            when(node.getStore()).thenReturn(mock(StoreEntity.class));
            when(node.getStore().getStoreRef()).thenReturn(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            when(node.getNodeRef()).thenReturn(new NodeRef("workspace", "SpacesStore", String.valueOf(i)));
            when(node.getTransaction()).thenReturn(mock(TransactionEntity.class));
            when(node.getTransaction().getId()).thenReturn((long) Math.floorDiv(i, NODES_PER_TRANSACTION)); // TODO: might be wrong.
            nodes.add(node);
        }

        when(nodeDAO.getMinTxnId()).thenReturn(0L);

        when(searchTrackingComponent.getMaxTxnId()).thenReturn((long) AMOUNT_OF_TRANSACTIONS);
        doAnswer(invocation -> {
            NodeParameters nodeParameters = invocation.getArgument(0);
            SearchTrackingComponent.NodeQueryCallback callback = invocation.getArgument(1);

            nodeParameters.getTransactionIds()
                    .stream()
                    .flatMap(index -> nodes.stream().filter(node -> Objects.equals(node.getTransaction().getId(), index)))
                    .forEach(callback::handleNode);
            return null;
        }).when(searchTrackingComponent).getNodes(any(), any());

        when(dummyJdbcTemplate.queryForList(anyString(), eq(Long.class))).thenAnswer(invocation -> {
            Matcher matcher = QUERY_PATTERN.matcher(invocation.getArgument(0));
            if (!matcher.matches()) throw new IllegalArgumentException(String.format("unexpected query: %s", invocation.getArgument(0)));
            int startTxnId = Integer.parseInt(matcher.group(1));
            int endTxnId = Integer.parseInt(matcher.group(2));
            int limit = Integer.parseInt(matcher.group(3));

            int end = Math.min(startTxnId + limit, Math.min(endTxnId, AMOUNT_OF_TRANSACTIONS));
            return LongStream.range(startTxnId, end).boxed().collect(Collectors.toList());
        });

        this.indexingStrategy = new TransactionAggregationIndexingStrategy(CONFIGURATION, nodeDAO, searchTrackingComponent, dummyJdbcTemplate);
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
        TransactionAggregationIndexingStrategyConfiguration configuration = new TransactionAggregationIndexingStrategyConfiguration(0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new TransactionAggregationIndexingStrategy(configuration, nodeDAO, searchTrackingComponent, dummyJdbcTemplate));
    }

}