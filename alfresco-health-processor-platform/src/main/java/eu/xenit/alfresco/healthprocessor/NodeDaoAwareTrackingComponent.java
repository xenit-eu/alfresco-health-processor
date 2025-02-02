package eu.xenit.alfresco.healthprocessor;

import eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent;
import org.alfresco.repo.domain.node.Transaction;

import java.util.List;

public interface NodeDaoAwareTrackingComponent extends TrackingComponent {
    int getTransactionCount();
    List<Transaction> getNextTransactions(Integer count);
}
