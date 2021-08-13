package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import org.alfresco.service.cmr.repository.NodeRef.Status;

public interface SolrNodeFilter {
    public boolean isIgnored(Status nodeRefStatus);

}
