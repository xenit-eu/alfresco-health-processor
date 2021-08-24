package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AbstractFactoryBean;

@AllArgsConstructor
@Slf4j
public class IndexingConfigurationFactoryBean extends AbstractFactoryBean<IndexingConfiguration> {

    private IndexingStrategy.IndexingStrategyKey indexingStrategyKey;

    private Collection<IndexingConfiguration> configurations;

    @Override
    public Class<?> getObjectType() {
        return IndexingConfiguration.class;
    }

    @Override
    protected IndexingConfiguration createInstance() {
        for (IndexingConfiguration configuration : configurations) {
            if (configuration.getIndexingStrategy() == indexingStrategyKey) {
                log.debug("Selected indexing configuration {}", configuration);
                return configuration;
            }
        }

        throw new IllegalStateException("No configuration found for indexing strategy: " + indexingStrategyKey);
    }
}
