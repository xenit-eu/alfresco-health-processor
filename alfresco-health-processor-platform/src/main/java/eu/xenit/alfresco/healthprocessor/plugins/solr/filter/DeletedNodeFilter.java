package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * Filter that ignores deleted nodes
 */
@Slf4j
@ToString
public class DeletedNodeFilter implements SolrNodeFilter {

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        if (nodeRefStatus.isDeleted()) {
            log.debug("Node {} ignored because it is deleted", nodeRefStatus.getNodeRef());
            return true;
        }
        return false;
    }
}
