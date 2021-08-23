package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.net.URI;
import java.util.Collections;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.junit.jupiter.api.Test;

class DbIdRangeSearchEndpointSelectorTest {

    private static final SearchEndpoint endpoint = new SearchEndpoint(URI.create("http://empty/"));

    private void rangeTests(SearchEndpointSelector endpointSelector) {
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
        SearchEndpointSelector endpointSelector = new DbIdRangeSearchEndpointSelector(50L, 100L, endpoint);

        rangeTests(endpointSelector);
    }

    @Test
    void testFilterParsing() {
        SearchEndpointSelector endpointSelector = new DbIdRangeSearchEndpointSelector("50-100", endpoint);

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
