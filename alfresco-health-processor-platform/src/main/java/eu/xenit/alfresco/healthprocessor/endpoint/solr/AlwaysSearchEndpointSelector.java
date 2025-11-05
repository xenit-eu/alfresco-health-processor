package eu.xenit.alfresco.healthprocessor.endpoint.solr;

import lombok.ToString;

/**
 * Simple endpoint selector that selects a single endpoint for all nodes
 */
@ToString(callSuper = true)
public class AlwaysSearchEndpointSelector
        extends eu.xenit.alfresco.healthprocessor.endpoint.AlwaysSearchEndpointSelector<SolrEndpoint>
{

    public AlwaysSearchEndpointSelector(String filter, SolrEndpoint searchEndpoint)
    {
        super(filter, searchEndpoint);
    }
}
