package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.net.URI;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class SearchEndpoint {

    private final String host;
    private final int port;
    private final String prefix;
    private final String core;

    public URI getBaseUri() {
        return URI.create("http://" + host + ":" + port + "/" + prefix + "/" + core);
    }
}
