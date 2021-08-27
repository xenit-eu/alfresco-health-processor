package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy.IndexingStrategyKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class IndexingStrategyKeyTest {
    @Test
    void fromKey() {
        assertEquals(IndexingStrategyKey.LAST_TXNS, IndexingStrategyKey.fromKey("last-txns"));
        assertEquals(IndexingStrategyKey.TXNID, IndexingStrategyKey.fromKey("txn-id"));
    }

    @Test
    void fromKey_invalid() {
        assertNull(IndexingStrategyKey.fromKey("invalid"));
    }

}
