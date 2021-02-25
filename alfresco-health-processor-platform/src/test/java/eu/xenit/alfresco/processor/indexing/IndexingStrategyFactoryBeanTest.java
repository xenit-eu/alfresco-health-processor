package eu.xenit.alfresco.processor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IndexingStrategyFactoryBeanTest {

    @Mock
    private TrackingComponent trackingComponent;

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
        return new IndexingStrategyFactoryBean(configuration, trackingComponent);
    }
}