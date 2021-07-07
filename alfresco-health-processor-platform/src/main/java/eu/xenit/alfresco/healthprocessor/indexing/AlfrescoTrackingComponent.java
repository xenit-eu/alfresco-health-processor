package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.alfresco.repo.solr.NodeParameters;
import org.alfresco.repo.solr.SOLRTrackingComponentImpl;

@AllArgsConstructor
public class AlfrescoTrackingComponent implements TrackingComponent {

    private final SOLRTrackingComponentImpl solrTrackingComponent;

    @Override
    public long getMaxTxnId() {
        return solrTrackingComponent.getMaxTxnId();
    }

    @Override
    public Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds) {
        Set<NodeInfo> ret = new HashSet<>();

        solrTrackingComponent.getNodes(nodeParameters(txnIds), node -> {
            // TODO filter out deleted nodes?
            ret.add(new NodeInfo(node.getTransaction().getId(), node.getId(), node.getNodeRef()));
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

    SOLRTrackingComponentImpl getSolrTrackingComponent() {
        return solrTrackingComponent;
    }
}
