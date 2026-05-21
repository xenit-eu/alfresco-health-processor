package eu.xenit.alfresco.healthprocessor.filter;

import org.alfresco.repo.event2.filter.NodeTypeFilter;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that ignores all nodes for which events could not have been emitted.
 * 
 * @author Axel Faust
 */
@Slf4j
@RequiredArgsConstructor
@ToString
public class NodeEventFilterBridge implements NodeFilter
{

    private final NodeService nodeService;

    private final NodeTypeFilter nodeTypeFilter;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIgnored(Status nodeRefStatus)
    {
        if (!nodeRefStatus.isDeleted())
        {
            QName type = this.nodeService.getType(nodeRefStatus.getNodeRef());
            if (nodeTypeFilter.isExcluded(type))
            {
                return true;
            }
        }
        return false;
    }

}
