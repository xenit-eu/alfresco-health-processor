package eu.xenit.alfresco.healthprocessor.endpoint.solr;

import java.net.URI;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A search endpoint is configuration data for access to a solr search index
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SolrEndpoint extends SearchEndpoint
{

    public SolrEndpoint(URI baseUri)
    {
        super(baseUri);
    }

    public URI getAdminUri()
    {
        return getBaseUri().resolve("../admin/");
    }

    public String getCoreName()
    {
        String[] path = getBaseUri().getPath().split("/");
        return path[path.length - 1];
    }
}
