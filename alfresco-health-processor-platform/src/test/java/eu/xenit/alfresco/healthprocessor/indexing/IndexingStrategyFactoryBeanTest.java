package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import eu.xenit.alfresco.healthprocessor.util.InMemoryAttributeStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private IndexingStrategyFactoryBean factoryBean() {
        return factoryBean(IndexingConfigUtil.defaultConfig());
    }

    private IndexingStrategyFactoryBean factoryBean(IndexingConfiguration configuration) {
        return new IndexingStrategyFactoryBean(configuration, trackingComponent, attributeStore);
    }
}