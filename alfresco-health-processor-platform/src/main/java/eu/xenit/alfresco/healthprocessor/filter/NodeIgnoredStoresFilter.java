package eu.xenit.alfresco.healthprocessor.filter;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.StoreRef;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that ignores all nodes that are in certain stores
 */
@Slf4j
@RequiredArgsConstructor
@ToString
public class NodeIgnoredStoresFilter implements NodeFilter {

    private final Collection<StoreRef> storeRefs;

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        return storeRefs.contains(nodeRefStatus.getNodeRef().getStoreRef());
    }
}
