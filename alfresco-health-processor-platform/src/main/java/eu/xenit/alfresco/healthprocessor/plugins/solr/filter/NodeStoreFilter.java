package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Filter that ignores all nodes that are NOT from a certain store
 */
@Slf4j
@RequiredArgsConstructor
public class NodeStoreFilter implements SolrNodeFilter {

    private final StoreRef storeRef;

    public NodeStoreFilter(String storeRef) {
        this(new StoreRef(storeRef));
    }

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        if (!nodeRefStatus.getNodeRef().getStoreRef().equals(storeRef)) {
            log.debug("Node {} ignored because it is not in store {}", nodeRefStatus.getNodeRef(), storeRef);
            return true;
        }
        return false;
    }
}
