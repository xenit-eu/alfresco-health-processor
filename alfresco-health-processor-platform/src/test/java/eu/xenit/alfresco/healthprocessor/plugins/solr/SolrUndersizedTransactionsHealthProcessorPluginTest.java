package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.google.common.collect.Sets;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.singletxns.SingleTransactionIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.singletxns.SingleTransactionIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import lombok.NonNull;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static eu.xenit.alfresco.healthprocessor.plugins.solr.SolrUndersizedTransactionsHealthProcessorPlugin.ARCHIVE_AND_WORKSPACE_STORE_REFS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SolrUndersizedTransactionsHealthProcessorPluginTest {

    private final static int THRESHOLD = 100;
    private final static boolean ENABLED = true;
    private final static @NonNull Properties PROPERTIES = new Properties(1);
    private final static int AMOUNT_OF_MERGER_THREADS = 1;

    private TransactionHelper transactionHelper;
    private ArrayList<NodeRef> processedNodes;
    private NodeService nodeService;
    private SolrUndersizedTransactionsHealthProcessorPlugin plugin;

    @BeforeAll
    static void beforeAll() {
        PROPERTIES.put("eu.xenit.alfresco.healthprocessor.indexing.strategy", IndexingStrategy.IndexingStrategyKey.SINGLE_TXNS.getKey());
    }

    @BeforeEach
    void setUp() {
        transactionHelper = mock(TransactionHelper.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionHelper).inNewTransaction(any(Runnable.class), eq(false));

        processedNodes = new ArrayList<>();
        nodeService = mock(NodeService.class);
        doAnswer(invocation -> {
            processedNodes.add(invocation.getArgument(0));
            return null;
        }).when(nodeService).addAspect(any(), eq(SolrUndersizedTransactionsHealthProcessorPlugin.ASPECT_QNAME),
                eq(Map.of()));

        this.plugin = new SolrUndersizedTransactionsHealthProcessorPlugin(PROPERTIES, ENABLED, THRESHOLD,
                AMOUNT_OF_MERGER_THREADS, transactionHelper, nodeService);
    }

    @Test
    void testLargeTransactionsAreNotProcessed() {
        Set<NodeRef> nodeRefs = generateRandomNodeRefs(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, THRESHOLD + 1);
        plugin.process(nodeRefs);

        verify(transactionHelper, never()).inNewTransaction(any(Runnable.class), anyBoolean());
        verify(nodeService, never()).setProperty(any(), any(), any());
    }

    @Test
    void testSmallTransactionsAreMerged() throws InterruptedException {
        Set<NodeRef> firstTransaction = generateRandomNodeRefs(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, THRESHOLD - 1);
        Set<NodeRef> secondTransaction = generateRandomNodeRefs(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 1);

        plugin.process(firstTransaction);
        verify(transactionHelper, never()).inNewTransaction(any(Runnable.class), anyBoolean());
        verify(nodeService, never()).setProperty(any(), any(), any());

        plugin.process(secondTransaction);
        for (int i = 0; i < 10; i ++) {
            if (plugin.queuedMergeRequests.get() == 0) break;
            Thread.sleep(1000);
        }
        if (plugin.queuedMergeRequests.get() != 0) fail("The merge request was not processed in time.");
        verify(transactionHelper, times(1)).inNewTransaction(any(Runnable.class), eq(false));
        assertEquals(Sets.union(firstTransaction, secondTransaction), Sets.newHashSet(processedNodes), "all nodes should be processed");
    }

    @Test
    void testGuaranteeSingleTransactionIndexerIsUsed() {
        Properties properties = new Properties();
        assertThrows(AssertionError.class, () -> new SolrUndersizedTransactionsHealthProcessorPlugin(properties, ENABLED, THRESHOLD, AMOUNT_OF_MERGER_THREADS, transactionHelper, nodeService));
        properties.put(SingleTransactionIndexingStrategy.selectedIndexingStrategyPropertyKey, "not single txns");
        assertThrows(AssertionError.class, () -> new SolrUndersizedTransactionsHealthProcessorPlugin(properties, ENABLED, THRESHOLD, AMOUNT_OF_MERGER_THREADS, transactionHelper, nodeService));
        properties.put(SingleTransactionIndexingStrategy.selectedIndexingStrategyPropertyKey, SingleTransactionIndexingStrategy.indexingStrategyKey.getKey());
        assertDoesNotThrow(() -> new SolrUndersizedTransactionsHealthProcessorPlugin(properties, ENABLED, THRESHOLD, AMOUNT_OF_MERGER_THREADS, transactionHelper, nodeService));
    }

    @Test
    void testNonWorkspaceAndArchiveNodesAreNotProcessed() throws InterruptedException {
        HashSet<NodeRef> firstTransaction = new HashSet<>(THRESHOLD);
        firstTransaction.addAll(generateRandomNodeRefs(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, THRESHOLD - 2));
        firstTransaction.addAll(generateRandomNodeRefs(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, 1));
        firstTransaction.addAll(generateRandomNodeRefs(new StoreRef("veryreal", "storeref"), 1));
        Set<NodeRef> secondTransaction = generateRandomNodeRefs(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 1);

        plugin.process(firstTransaction);
        verify(transactionHelper, never()).inNewTransaction(any(Runnable.class), anyBoolean());
        verify(nodeService, never()).setProperty(any(), any(), any());

        plugin.process(secondTransaction);
        plugin.process(secondTransaction);
        for (int i = 0; i < 10; i ++) {
            if (plugin.queuedMergeRequests.get() == 0) break;
            Thread.sleep(1000);
        }
        if (plugin.queuedMergeRequests.get() != 0) fail("The merge request was not processed in time.");
        verify(transactionHelper, times(1)).inNewTransaction(any(Runnable.class), eq(false));
        assertEquals(Sets.union(firstTransaction.stream()
                .filter(nodeRef -> ARCHIVE_AND_WORKSPACE_STORE_REFS.contains(nodeRef.getStoreRef()))
                .collect(Collectors.toSet()), secondTransaction),
                Sets.newHashSet(processedNodes), "only workspace and archive nodes should be processed");
    }

    @Test
    void testIndexerCallbacks() {
        SingleTransactionIndexingStrategy strategy = new SingleTransactionIndexingStrategy(mock(TrackingComponent.class),
                new SingleTransactionIndexingConfiguration(1, 1, 1));
        assertEquals("false", plugin.getState().get("isRunning")); // Normally, this is only called by the UI.
        strategy.onStart();
        assertEquals("true", plugin.getState().get("isRunning"));
        strategy.onStop();
        assertEquals("false", plugin.getState().get("isRunning"));
    }

    public static @NonNull Set<NodeRef> generateRandomNodeRefs(@NonNull StoreRef storeRef, int amount) {
        return IntStream.range(0, amount)
                .mapToObj(unused -> UUID.randomUUID())
                .map(uuid -> new NodeRef(storeRef, uuid.toString()))
                .collect(Collectors.toSet());
    }
}