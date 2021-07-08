package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.List;
import java.util.Set;
import lombok.Value;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.service.cmr.repository.NodeRef;

public interface TrackingComponent {

    long getMaxTxnId();

    Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds);

    default NodeParameters toNodeParameters(List<Long> txnIds) {
        final NodeParameters ret = new NodeParameters();
        ret.setTransactionIds(txnIds);
        // TODO support additional configuration: storeProtocol, storeIdentifier, includeNodeTypes, excludeNodeTypes, ...
        return ret;
    }

    @Value
    class NodeInfo {

        long txnId;
        long nodeId;
        NodeRef nodeRef;
    }

}
