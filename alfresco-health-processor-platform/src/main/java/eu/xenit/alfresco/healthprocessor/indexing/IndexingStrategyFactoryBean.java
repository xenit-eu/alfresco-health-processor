package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.lasttxns.LastTxnsBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.lasttxns.LastTxnsIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.txnaggregation.TransactionAggregationIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.txnaggregation.TransactionAggregationIndexingStrategyConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdIndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.util.AttributeStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.sql.DataSource;

@AllArgsConstructor
public final class IndexingStrategyFactoryBean extends AbstractFactoryBean<IndexingStrategy> {

    private final IndexingConfiguration configuration;
    private final TrackingComponent trackingComponent;
    private final AttributeStore attributeStore;
    private final SearchTrackingComponent searchTrackingComponent;
    private final AbstractNodeDAOImpl nodeDAO;
    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;

    @Override
    public Class<?> getObjectType() {
        return IndexingStrategy.class;
    }

    @Override
    protected IndexingStrategy createInstance() {
        return createIndexingStrategy(configuration.getIndexingStrategy());
    }

    private IndexingStrategy createIndexingStrategy(IndexingStrategy.IndexingStrategyKey indexingStrategy) {
        switch(indexingStrategy) {
            case TXNID:
                return new TxnIdBasedIndexingStrategy((TxnIdIndexingConfiguration) configuration, trackingComponent, attributeStore);
            case LAST_TXNS:
                return new LastTxnsBasedIndexingStrategy((LastTxnsIndexingConfiguration) configuration, trackingComponent);
            case TXN_AGGREGATION:
                return new TransactionAggregationIndexingStrategy((TransactionAggregationIndexingStrategyConfiguration) configuration, nodeDAO,
                        searchTrackingComponent, dataSource, meterRegistry);
            default:
                throw new IllegalArgumentException("Unknown indexing strategy: "+ indexingStrategy);
        }
    }
}
