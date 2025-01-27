package eu.xenit.alfresco.healthprocessor.indexing;

import static eu.xenit.alfresco.healthprocessor.util.SetUtil.set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy.IndexingStrategyKey;
import eu.xenit.alfresco.healthprocessor.indexing.lasttxns.LastTxnsIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.singletxns.SingleTransactionIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdIndexingConfiguration;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class IndexingConfigurationFactoryBeanTest {
    @Test
    void selectConfigurationForIndexingKey() {
        assertThat(factoryBean(IndexingStrategyKey.TXNID).createInstance(), is(instanceOf(TxnIdIndexingConfiguration.class)));
        assertThat(factoryBean(IndexingStrategyKey.LAST_TXNS).createInstance(), is(instanceOf(LastTxnsIndexingConfiguration.class)));
        assertThat(factoryBean(IndexingStrategyKey.SINGLE_TXNS).createInstance(), is(instanceOf(SingleTransactionIndexingConfiguration.class)));
    }

    @Test
    void selectConfiguration_nonExisting() {
        IndexingConfigurationFactoryBean factoryBean = new IndexingConfigurationFactoryBean(IndexingStrategyKey.TXNID,
                Collections.emptyList());
        assertThrows(IllegalStateException.class, factoryBean::createInstance);
    }

    IndexingConfigurationFactoryBean factoryBean(IndexingStrategyKey indexingStrategyKey) {
        return new IndexingConfigurationFactoryBean(indexingStrategyKey, set(
                new LastTxnsIndexingConfiguration(1,1),
                new TxnIdIndexingConfiguration(1,1,1),
                new SingleTransactionIndexingConfiguration(1,1)));
    }
}
