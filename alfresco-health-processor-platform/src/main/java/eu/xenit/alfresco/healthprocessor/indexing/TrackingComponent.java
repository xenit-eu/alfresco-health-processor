package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.List;
import java.util.Set;
import org.alfresco.service.cmr.repository.NodeRef;

public interface TrackingComponent {

    long getMaxTxnId();

    Set<NodeRef> getNodesForTxnIds(List<Long> txnIds);

}
