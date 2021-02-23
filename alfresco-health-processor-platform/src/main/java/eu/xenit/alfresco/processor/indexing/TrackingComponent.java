package eu.xenit.alfresco.processor.indexing;

import java.util.List;
import java.util.Set;

public interface TrackingComponent {

    long getMaxTxnId();

    Set<Long> getNodesForTxnIds(List<Long> txnIds);

}
