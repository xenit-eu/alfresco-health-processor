package eu.xenit.alfresco.healthprocessor.checker.solr.filter;

import org.alfresco.service.cmr.repository.NodeRef.Status;

/**
 * A filter that is used to determine if a certain node should be ignored.
 */
public interface SolrNodeFilter {

    boolean isIgnored(Status nodeRefStatus);
}
