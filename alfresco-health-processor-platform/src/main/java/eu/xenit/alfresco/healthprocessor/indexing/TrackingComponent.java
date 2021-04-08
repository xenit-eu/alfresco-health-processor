package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.List;
import java.util.Set;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

public interface TrackingComponent {

    long getMaxTxnId();

    Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds);

    @Value
    class NodeInfo implements Comparable<NodeInfo> {

        long txnId;
        long nodeId;
        NodeRef nodeRef;

        @Override
        public int compareTo(NodeInfo other) {
            return Long.compare(txnId, other.txnId);
        }
    }

}
