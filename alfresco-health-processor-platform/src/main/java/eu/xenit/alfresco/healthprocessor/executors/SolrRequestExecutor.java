package eu.xenit.alfresco.healthprocessor.executors;

import java.io.IOException;
import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef.Status;

import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Performs HTTP requests on a {@link SolrEndpoint}
 */
public interface SolrRequestExecutor
{

    /**
     * The boolean targetsTransaction indicates if the action should be sent for the transaction the node was contained in.
     * If true, the nodeCommand will be scheduled for the complete transaction of this node.
     * If false, the nodeCommand is scheduled for this single node contained in the nodestatus.
     */
    @AllArgsConstructor
    public enum SolrNodeCommand
    {

        REINDEX("reindex", false),
        PURGE("purge", false),
        REINDEX_TRANSACTION("reindex", true);

        @Getter
        private final String command;

        @Getter
        private final boolean targetsTransaction;
    }

    @Value
    public static class SolrActionResponse
    {

        private final boolean successFull;

        private final String message;
    }

    /**
     * Performs a search operation on an endpoint to determine if the nodes are indexed or not
     *
     * @param endpoint
     *     The endpoint to perform a search on
     * @param nodeStatuses
     *     Nodes to search for
     * @return The result of the search operation
     * @throws IOException
     *     When the HTTP request goes wrong
     */
    public SolrSearchResult checkNodeIndexed(SolrEndpoint endpoint, Collection<Status> nodeStatuses) throws IOException;

    /**
     * Schedules an async SolrNodeCommand for a node on a search endpoint.
     * This action/command is scheduled for execution by solr or a failure is returned.
     * 
     * @param endpoint
     *     the search endpoint
     * @param nodeStatus
     *     node status containing information about the dbIDs and transactionIds
     * @param command
     *     Solr action that will be executed
     * @return the action response
     * @throws IOException
     *     when the command can not be sent to solr
     */
    public SolrActionResponse executeAsyncNodeCommand(SolrEndpoint endpoint, Status nodeStatus, SolrNodeCommand command) throws IOException;
}
