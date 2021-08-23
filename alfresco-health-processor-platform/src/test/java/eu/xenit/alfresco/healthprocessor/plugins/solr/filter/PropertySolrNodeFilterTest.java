package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertySolrNodeFilterTest {

    @Mock
    private NodeService nodeService;

    private SolrNodeFilter filter;

    @BeforeEach
    void setup() {
        Map<QName, Serializable> propertyMap = new HashMap<>();
        propertyMap.put(ContentModel.PROP_IS_INDEXED, false);
        propertyMap.put(ContentModel.PROP_CREATOR, "System");
        filter = new PropertySolrNodeFilter(nodeService, propertyMap);
    }

    @Test
    void testPropertyFilterNotMatching() {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_CREATOR, "admin");
        properties.put(ContentModel.PROP_AUTHOR, "Lars");

        when(nodeService.getProperties(TestNodeRefs.REF)).thenReturn(properties);

        assertFalse(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testPropertyFilterMatchingString() {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_CREATOR, "System");
        properties.put(ContentModel.PROP_AUTHOR, "Lars");

        when(nodeService.getProperties(TestNodeRefs.REF)).thenReturn(properties);

        assertTrue(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testPropertyFilterMatchingBoolean() {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_IS_INDEXED, false);
        properties.put(ContentModel.PROP_CREATOR, "admin");
        properties.put(ContentModel.PROP_AUTHOR, "Lars");

        when(nodeService.getProperties(TestNodeRefs.REF)).thenReturn(properties);

        assertTrue(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testPropertyFilterMatchingList() {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_CREATOR, new ArrayList<>(Arrays.asList("admin", "System")));
        properties.put(ContentModel.PROP_AUTHOR, "Lars");

        when(nodeService.getProperties(TestNodeRefs.REF)).thenReturn(properties);

        assertTrue(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testPropertyFilterNotMatchingList() {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_CREATOR, new ArrayList<>(Arrays.asList("admin", "Lars")));
        properties.put(ContentModel.PROP_AUTHOR, "Lars");

        when(nodeService.getProperties(TestNodeRefs.REF)).thenReturn(properties);

        assertFalse(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }

    @Test
    void testPropertyFilterSkipsDeletedNodes() {
        assertFalse(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, true)));
    }

    @Test
    void testPropertyFilterSkipsNodesThatThrowException() {
        when(nodeService.getProperties(TestNodeRefs.REF)).thenThrow(new InvalidNodeRefException(TestNodeRefs.REF));
        assertFalse(filter.isIgnored(new Status(1L, TestNodeRefs.REF, "1", 1L, false)));
    }
}
