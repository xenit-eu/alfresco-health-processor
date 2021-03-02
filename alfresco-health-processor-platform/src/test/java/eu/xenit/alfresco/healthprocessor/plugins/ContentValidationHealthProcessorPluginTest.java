package eu.xenit.alfresco.healthprocessor.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentValidationHealthProcessorPluginTest {

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
        assertThat(plugin.process(NODE_REF), is(nullValue()));

        when(nodeService.exists(NODE_REF)).thenReturn(true);
        when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, true));
        assertThat(plugin.process(NODE_REF), is(nullValue()));
    }

    @Test
    void process_propertyHasNoContentReader() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.exists(NODE_REF)).thenReturn(true);
        when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, false));
        when(contentService.getReader(NODE_REF, Q_NAME)).thenReturn(null);

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.NONE)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_propertyHasContentReader_contentExists() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.exists(NODE_REF)).thenReturn(true);
        when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, false));
        when(contentService.getReader(NODE_REF, Q_NAME)).thenReturn(new MockContentReader(true));

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.HEALTHY)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
    }

    @Test
    void process_propertyHasContentReader_contentDoesNotExist() {
        ContentValidationHealthProcessorPlugin plugin = initialize(Collections.singletonList(Q_NAME));

        when(nodeService.exists(NODE_REF)).thenReturn(true);
        when(nodeService.getNodeStatus(NODE_REF)).thenReturn(new Status(100L, NODE_REF, null, 10L, false));
        when(contentService.getReader(NODE_REF, Q_NAME)).thenReturn(new MockContentReader(false));

        NodeHealthReport report = plugin.process(NODE_REF);
        assertThat(report, is(notNullValue()));
        assertThat(report.getStatus(), is(equalTo(NodeHealthStatus.UNHEALTHY)));
        assertThat(report.getNodeRef(), is(equalTo(NODE_REF)));
        assertThat(report.getMessages(), contains(Q_NAME.toString()));
    }

    private ContentValidationHealthProcessorPlugin initialize(Collection<QName> propertyQNamesToValidate) {
        return new ContentValidationHealthProcessorPlugin(nodeService, contentService, dictionaryService,
                propertyQNamesToValidate);
    }

    @AllArgsConstructor
    private static class MockContentReader implements ContentReader {

        private final boolean exists;

        @Override
        public ContentReader getReader() throws ContentIOException {
            return null;
        }

        @Override
        public boolean exists() {
            return exists;
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public ReadableByteChannel getReadableChannel() throws ContentIOException {
            return null;
        }

        @Override
        public FileChannel getFileChannel() throws ContentIOException {
            return null;
        }

        @Override
        public InputStream getContentInputStream() throws ContentIOException {
            return null;
        }

        @Override
        public void getContent(OutputStream os) throws ContentIOException {

        }

        @Override
        public void getContent(File file) throws ContentIOException {

        }

        @Override
        public String getContentString() throws ContentIOException {
            return null;
        }

        @Override
        public String getContentString(int length) throws ContentIOException {
            return null;
        }

        @Override
        public boolean isChannelOpen() {
            return false;
        }

        @Override
        public void addListener(ContentStreamListener listener) {

        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public ContentData getContentData() {
            return null;
        }

        @Override
        public String getContentUrl() {
            return null;
        }

        @Override
        public String getMimetype() {
            return null;
        }

        @Override
        public void setMimetype(String mimetype) {

        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(Locale locale) {

        }
    }

}