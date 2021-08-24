package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.Map;
import javax.annotation.Nonnull;

public interface IndexingConfiguration {

    @Nonnull
    IndexingStrategy.IndexingStrategyKey getIndexingStrategy();

    @Nonnull
    Map<String, String> getConfiguration();

}
