package eu.xenit.alfresco.healthprocessor.endpoint.elastic;

import java.net.URI;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A search endpoint is configuration data for access to an ElasticSearch index
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ElasticEndpoint extends SearchEndpoint
{
    @Getter
    private final boolean expectPathsIndexed;

    public ElasticEndpoint(String secureComms, String baseUri, boolean expectPathsIndexed)
    {
        super(URI.create((secureComms.equals("https") ? "https" : "http") + "://" + baseUri));
        this.expectPathsIndexed = expectPathsIndexed;
    }
}
