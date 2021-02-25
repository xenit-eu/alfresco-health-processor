package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration.IndexingStrategyKey;
import org.junit.jupiter.api.Test;

class IndexingConfigurationTest {

    @Test
    void initialize_indexingStrategyKeyFromString() {
        IndexingConfiguration config = new IndexingConfiguration("txn-id", -1L, Long.MAX_VALUE, 1000);

        assertThat(config.getIndexingStrategy(), is(equalTo(IndexingStrategyKey.TXNID)));
    }

}