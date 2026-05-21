package eu.xenit.alfresco.healthprocessor.executors;

import java.io.IOException;
import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef.Status;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import lombok.Value;

/**
 * Performs HTTP requests on a ElasticSearch {@link SearchEndpoint}
 */
public interface ElasticRequestExecutor
{

    @Value
    public static class ActionResponse
    {

        private final boolean successFull;

        private final String message;
    }

    /**
     * Performs a check operation on an endpoint to determine if the nodes are properly indexed or not
     *
     * @param endpoint
     *     The endpoint to perform a search on
     * @param nodeStatuses
     *     Nodes to search for
     * @return The result of the check operation
     * @throws IOException
     *     When the HTTP request goes wrong
     */
    public ElasticResult checkNodeIndexed(SearchEndpoint endpoint, Collection<Status> nodeStatuses) throws IOException;

    /**
     * Updates the {@code ALIVE} flag in an ElasticSearch index entry. Note: The ALIVE state is used to count indexed documents as reported
     * in Admin Console, but index entries with {@code ALIVE=false} may be found with index queries while they are not included in the
     * count.
     * 
     * @param endpoint
     *     the endpoint on which to update the index entry
     * @param id
     *     the ID of the NodeRef for which to update the index entry
     * @param alive
     *     {@code true} if the index entry should be marked as alive or {@code false} if not
     * @return the action response
     * @throws IOException
     *     When the HTTP request goes wrong
     */
    public ActionResponse updateDocumentAliveState(SearchEndpoint endpoint, String id, boolean alive) throws IOException;

    /**
     * Deletes a node index entry.
     * 
     * @param endpoint
     *     the endpoint on which to delete the index entry
     * @param id
     *     the ID for the NodeRef for which to delete the index entry
     * @return the action response
     * @throws IOException
     *     When the HTTP request goes wrong
     */
    public ActionResponse deleteNodeIndexEntry(SearchEndpoint endpoint, String id) throws IOException;
}
