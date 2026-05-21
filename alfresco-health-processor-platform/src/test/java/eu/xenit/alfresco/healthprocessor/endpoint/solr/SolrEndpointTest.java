package eu.xenit.alfresco.healthprocessor.endpoint.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

class SolrEndpointTest {

    @Test
    void createWithEndingSlash() {
        SolrEndpoint endpoint = new SolrEndpoint(URI.create("http://empty/abc/"));

        assertEquals(URI.create("http://empty/abc/"), endpoint.getBaseUri());
    }

    @Test
    void createWithoutEndingSlash() {
        SolrEndpoint endpoint = new SolrEndpoint(URI.create("http://empty/abc"));

        assertEquals(URI.create("http://empty/abc/"), endpoint.getBaseUri());
    }

}
