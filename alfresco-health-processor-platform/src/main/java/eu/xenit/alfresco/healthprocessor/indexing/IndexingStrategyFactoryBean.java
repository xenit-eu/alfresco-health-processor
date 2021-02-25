package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration.IndexingStrategyKey;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;

@AllArgsConstructor
public final class IndexingStrategyFactoryBean extends AbstractFactoryBean<IndexingStrategy> {

    private final IndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;

    @Override
    public Class<?> getObjectType() {
        return IndexingStrategy.class;
    }

    @Override
    protected IndexingStrategy createInstance() {
        return createIndexingStrategy(configuration.getIndexingStrategy());
    }

    private IndexingStrategy createIndexingStrategy(IndexingStrategyKey ignoreForNow) {
        return new TxnIdBasedIndexingStrategy(configuration, trackingComponent);
    }
}