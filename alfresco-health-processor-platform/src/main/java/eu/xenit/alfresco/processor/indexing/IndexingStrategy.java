package eu.xenit.alfresco.processor.indexing;

import java.util.Set;

public interface IndexingStrategy {

    void reset();

    Set<Long> getNextNodeIds(final int amount);


}
