package eu.xenit.alfresco.processor.indexing;

import eu.xenit.alfresco.processor.PropertyConstants;
import eu.xenit.alfresco.processor.PropertyConstants.IndexingStrategyKey;
import java.util.Properties;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;

@AllArgsConstructor
public final class IndexingStrategyFactoryBean extends AbstractFactoryBean<IndexingStrategy> {

    private final Properties globalProperties;
    private final TrackingComponent trackingComponent;

    @Override
    public Class<?> getObjectType() {
        return IndexingStrategy.class;
    }

    @Override
    protected IndexingStrategy createInstance() {
        return createIndexingStrategy(getIndexingStrategyKey());
    }

    private IndexingStrategyKey getIndexingStrategyKey() {
        return IndexingStrategyKey.fromKey(globalProperties.getProperty(PropertyConstants.PROP_INDEXING_STRATEGY));
    }

    private IndexingStrategy createIndexingStrategy(IndexingStrategyKey ignoreForNow) {
        return new TxnIdBasedIndexingStrategy(globalProperties, trackingComponent);
    }
}
