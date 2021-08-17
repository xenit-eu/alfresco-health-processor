package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration.IndexingStrategyKey;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;

@AllArgsConstructor
public final class IndexingStrategyFactoryBean extends AbstractFactoryBean<IndexingStrategy> {

    private final IndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;
    private final AttributeStore attributeStore;

    @Override
    public Class<?> getObjectType() {
        return IndexingStrategy.class;
    }

    @Override
    protected IndexingStrategy createInstance() {
        return createIndexingStrategy(configuration.getIndexingStrategy());
    }

    private IndexingStrategy createIndexingStrategy(IndexingStrategyKey indexingStrategy) {
        switch(indexingStrategy) {
            case TXNID:
                return new TxnIdBasedIndexingStrategy(configuration, trackingComponent, attributeStore);
            default:
                throw new IllegalArgumentException("Unknown indexing strategy: "+ Objects.toString(indexingStrategy));
        }
    }
}
