package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import eu.xenit.alfresco.healthprocessor.indexing.lasttxns.LastTxnsBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.lasttxns.LastTxnsIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import eu.xenit.alfresco.healthprocessor.util.InMemoryAttributeStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

@ExtendWith(MockitoExtension.class)
public class IndexingStrategyFactoryBeanTest {

    @Mock
    private TrackingComponent trackingComponent;
    private AttributeStore attributeStore = new InMemoryAttributeStore();

    @Test
    void getObjectType() {
        assertThat(factoryBean().getObjectType(), is(equalTo(IndexingStrategy.class)));
    }

    @Test
    void createInstance() {
        assertThat(factoryBean().createInstance(), is(instanceOf(IndexingStrategy.class)));
    }

    @Test
    void createInstanceForLastTxns() {
        assertThat(
                factoryBean(new LastTxnsIndexingConfiguration(1, 1)).createInstance(),
                is(instanceOf(LastTxnsBasedIndexingStrategy.class))
        );
    }

    @Test
    void createInstanceForTxnId() {
        assertThat(
                factoryBean(new TxnIdIndexingConfiguration(1, 1, 1)).createInstance(),
                is(instanceOf(TxnIdBasedIndexingStrategy.class))
        );
    }

    private IndexingStrategyFactoryBean factoryBean() {
        return factoryBean(IndexingConfigUtil.defaultConfig());
    }

    private IndexingStrategyFactoryBean factoryBean(IndexingConfiguration configuration) {
        return new IndexingStrategyFactoryBean(configuration, trackingComponent, attributeStore,
                mock(SearchTrackingComponent.class), mock(AbstractNodeDAOImpl.class), mock(DataSource.class), mock(MeterRegistry.class));
    }
}
