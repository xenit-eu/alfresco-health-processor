package eu.xenit.alfresco.healthprocessor.executors;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Result from an Elastic document node check operation
 */
@Value
@AllArgsConstructor
public class ElasticResult
{

    public ElasticResult()
    {
        this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * Nodes that have been found by the search operation
     */
    Set<NodeRef.Status> found;

    /**
     * Nodes that have been found by the search operation but with an outdated state
     */
    Set<NodeRef.Status> outdated;

    /**
     * Nodes that have been found by the search operation but without a PATH field
     */
    Set<NodeRef.Status> pathMissing;

    /**
     * Nodes that have been found but should not have been in the index
     */
    Set<NodeRef.Status> superflous;

    /**
     * Nodes that were expected to be indexed, but were not found in the index
     */
    Set<NodeRef.Status> missing;
}
