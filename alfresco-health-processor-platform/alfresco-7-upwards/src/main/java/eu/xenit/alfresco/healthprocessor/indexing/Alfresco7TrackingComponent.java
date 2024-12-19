package eu.xenit.alfresco.healthprocessor.indexing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.alfresco.repo.search.SearchTrackingComponent;
import org.alfresco.repo.solr.NodeParameters;

@AllArgsConstructor
public class Alfresco7TrackingComponent implements TrackingComponent {

    private final SearchTrackingComponent trackingComponent;

    @Override
    public long getMaxTxnId() {
        return trackingComponent.getMaxTxnId();
    }

    @Override
    public Set<NodeInfo> getNodesForTxnIds(List<Long> txnIds) {
        if(txnIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<NodeInfo> ret = new HashSet<>();
        NodeParameters parameters = toNodeParameters(txnIds);
        trackingComponent.getNodes(parameters, node -> {
            // TODO filter out deleted nodes?
            ret.add(new NodeInfo(node.getTransaction().getId(), node.getId(), node.getNodeRef()));
            return true;
        });

        return ret;
    }
}
