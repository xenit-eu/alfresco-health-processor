package eu.xenit.alfresco.healthprocessor.checker.solr.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

import eu.xenit.alfresco.healthprocessor.checker.solr.endpoint.SearchEndpoint;

class SearchEndpointTest {

    @Test
    void createWithEndingSlash() {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/abc/"));

        assertEquals(URI.create("http://empty/abc/"), endpoint.getBaseUri());
    }

    @Test
    void createWithoutEndingSlash() {
        SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/abc"));

        assertEquals(URI.create("http://empty/abc/"), endpoint.getBaseUri());
    }

}
