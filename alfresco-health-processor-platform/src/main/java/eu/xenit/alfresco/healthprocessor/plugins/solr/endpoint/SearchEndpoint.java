package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.net.URI;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A search endpoint is configuration data for access to a solr search index
 */
@EqualsAndHashCode
@ToString
public class SearchEndpoint {

    @Getter
    private final URI baseUri;

    public SearchEndpoint(URI baseUri) {
        if (!baseUri.getPath().endsWith("/")) {
            this.baseUri = URI.create(baseUri + "/");
        } else {
            this.baseUri = baseUri;
        }
    }
}
