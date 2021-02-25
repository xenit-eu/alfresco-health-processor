package eu.xenit.alfresco.processor.indexing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.service.cmr.repository.NodeRef;

@AllArgsConstructor
public class AlfrescoTrackingComponent implements TrackingComponent {


    private final SOLRTrackingComponent solrTrackingComponent;

    @Override
    public long getMaxTxnId() {
        return solrTrackingComponent.getMaxTxnId();
    }

    @Override
    public Set<NodeRef> getNodesForTxnIds(List<Long> txnIds) {
        Set<NodeRef> ret = new HashSet<>();

        solrTrackingComponent.getNodes(nodeParameters(txnIds), node -> {
            // TODO filter out deleted nodes?
            ret.add(node.getNodeRef());
            return true;
        });

        return ret;
    }

    private NodeParameters nodeParameters(List<Long> txnIds) {
        final NodeParameters ret = new NodeParameters();
        ret.setTransactionIds(txnIds);
        // TODO support additional configuration: storeProtocol, storeIdentifier, includeNodeTypes, excludeNodeTypes, ...
        return ret;
    }
}
