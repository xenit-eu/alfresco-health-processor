package eu.xenit.alfresco.healthprocessor.filter;

import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * A filter that is used to determine if a certain node should be ignored.
 */
public interface NodeFilter {

    boolean isIgnored(Status nodeRefStatus);
}
