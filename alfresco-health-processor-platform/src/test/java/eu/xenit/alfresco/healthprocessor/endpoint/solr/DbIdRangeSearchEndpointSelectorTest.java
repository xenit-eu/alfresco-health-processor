package eu.xenit.alfresco.healthprocessor.endpoint.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Collections;

import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.junit.jupiter.api.Test;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;

class DbIdRangeSearchEndpointSelectorTest {

    private static final SolrEndpoint endpoint = new SolrEndpoint(URI.create("http://empty/"));

    private void rangeTests(SearchEndpointSelector<SolrEndpoint> endpointSelector) {
        assertEquals(Collections.emptySet(), endpointSelector.getSearchEndpointsForNode(new Status(1L,
                TestNodeRefs.REF, "1", 1L, false)));
        assertEquals(Collections.emptySet(), endpointSelector.getSearchEndpointsForNode(new Status(49L,
                TestNodeRefs.REF, "1", 1L, false)));
        assertEquals(Collections.singleton(endpoint), endpointSelector.getSearchEndpointsForNode(new Status(50L,
                TestNodeRefs.REF, "1", 1L, false)));
        assertEquals(Collections.singleton(endpoint), endpointSelector.getSearchEndpointsForNode(new Status(80L,
                TestNodeRefs.REF, "1", 1L, false)));
        assertEquals(Collections.emptySet(), endpointSelector.getSearchEndpointsForNode(new Status(100L,
                TestNodeRefs.REF, "1", 1L, false)));
        assertEquals(Collections.emptySet(), endpointSelector.getSearchEndpointsForNode(new Status(200L,
                TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testSelectsInRange() {
        SearchEndpointSelector<SolrEndpoint> endpointSelector = new DbIdRangeSearchEndpointSelector(50L, 100L, endpoint);

        rangeTests(endpointSelector);
    }

    @Test
    void testFilterParsing() {
        SearchEndpointSelector<SolrEndpoint> endpointSelector = new DbIdRangeSearchEndpointSelector("50-100", endpoint);

        rangeTests(endpointSelector);
    }

    @Test
    void testInvalidFilter() {
        assertThrows(NumberFormatException.class, () -> {
            new DbIdRangeSearchEndpointSelector("50-", endpoint);
        });

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            new DbIdRangeSearchEndpointSelector("50", endpoint);
        });

        assertThrows(NumberFormatException.class, () -> {
            new DbIdRangeSearchEndpointSelector("", endpoint);
        });

        assertThrows(NumberFormatException.class, () -> {
            new DbIdRangeSearchEndpointSelector("abc-100", endpoint);
        });

        assertThrows(NumberFormatException.class, () -> {
            new DbIdRangeSearchEndpointSelector("abc", endpoint);
        });

    }

}
