package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Strategy used by the Health-Processor to iterate over (a subset of) Alfresco nodes. Once {@link #getNextNodeIds(int)}
 * returns an empty set, the Health-Processor considers the iteration done and the current cycle will be terminated.
 * Implementations can use the {@link #onStart()} and {@link #onStart()} to (re-)initialize state or open and close
 * additional resources.
 *
 * @see eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategyFactoryBean
 */
public interface IndexingStrategy {

    default void onStart() {

    }

    @Nonnull
    Set<NodeRef> getNextNodeIds(final int amount);

    default void onStop() {

    }

    @Nonnull
    default Map<String, String> getState() {
        return new HashMap<>();
    }

}
