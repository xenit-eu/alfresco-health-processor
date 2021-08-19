package eu.xenit.alfresco.healthprocessor.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentValidationHealthProcessorPluginTest {

    private static final String CONTENT_URL = "s3://abc.bin";
    private static final QName Q_NAME = QName.createQName("foobar", "baz");
    private static final NodeRef NODE_REF = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
            UUID.randomUUID().toString());

    @Mock
    private ServiceRegistry serviceRegistry;
    @Mock
    private NodeService nodeService;
    @Mock
    private ContentService contentService;
    @Mock
    private DictionaryService dictionaryService;
    @Mock
    private ContentReader contentReader;

    @BeforeEach
    void setup() {
        lenient().when(nodeService.exists(NODE_REF)).thenReturn(true);
        lenient().when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, false));
        lenient().when(nodeService.getProperties(NODE_REF)).thenReturn(new HashMap<QName, Serializable>() {{
            put(Q_NAME, new ContentData(CONTENT_URL, "plain/text", 10, "UTF-8"));
        }});
        lenient().when(contentService.getRawReader(CONTENT_URL)).thenReturn(contentReader);
    }

    @Test
    void initialize_usingServiceRegistry() {
        when(serviceRegistry.getNodeService()).thenReturn(nodeService);
        when(serviceRegistry.getContentService()).thenReturn(contentService);
        when(serviceRegistry.getDictionaryService()).thenReturn(dictionaryService);

        ContentValidationHealthProcessorPlugin plugin =
                new ContentValidationHealthProcessorPlugin(serviceRegistry, Collections.singletonList("{foobar}baz"));

        assertThat(plugin.getPropertyQNamesToValidate(), contains(Q_NAME));
    }

    @Test
    void initialize_noPropertyQNamesProvided() {
        when(dictionaryService.getAllProperties(DataTypeDefinition.CONTENT))
                .thenReturn(Collections.singletonList(Q_NAME));

        assertThat(initialize(Collections.emptyList()).getPropertyQNamesToValidate(), contains(Q_NAME));
        verify(dictionaryService).getAllProperties(DataTypeDefinition.CONTENT);

        assertThat(initialize(null).getPropertyQNamesToValidate(), contains(Q_NAME));
        verify(dictionaryService, times(2)).getAllProperties(DataTypeDefinition.CONTENT);

        verifyNoMoreInteractions(dictionaryService);
    }

    @Test
    void process_nodeDoesNotExist() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.exists(NODE_REF)).thenReturn(false);
        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));

        when(nodeService.exists(NODE_REF)).thenReturn(true);
        when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, true));

        report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_propertyHasNoContentReader() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(contentService.getRawReader(CONTENT_URL)).thenReturn(null);

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.UNHEALTHY)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_nodeDoesNotHaveApplicableProperty() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.getProperties(NODE_REF)).thenReturn(new HashMap<>());

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_nodeHasApplicableProperty_withNullValue() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.getProperties(NODE_REF)).thenReturn(new HashMap<QName, Serializable>() {{
            put(Q_NAME, null);
        }});

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_nodeHasApplicableProperty_withInvalidPropertyType() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.getProperties(NODE_REF)).thenReturn(new HashMap<QName, Serializable>() {{
            put(Q_NAME, "Invalid Type (expected d:content)");
        }});

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_nodeHasApplicableProperty_withNoContentUrl() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.getProperties(NODE_REF)).thenReturn(new HashMap<QName, Serializable>() {{
            put(Q_NAME, new ContentData(null, "plain/text", 10, "UTF-8"));
        }});

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_propertyHasContentReader_contentExists() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(contentReader.exists()).thenReturn(true);

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.HEALTHY)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_propertyHasContentReader_contentDoesNotExist() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(contentReader.exists()).thenReturn(false);

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.UNHEALTHY)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
        assertThat(report.getMessages(), contains(containsString(Q_NAME.toString())));
    }

    private ContentValidationHealthProcessorPlugin initialize(Collection<QName> propertyQNamesToValidate) {
        return new ContentValidationHealthProcessorPlugin(nodeService, contentService, dictionaryService,
                propertyQNamesToValidate);
    }

}
