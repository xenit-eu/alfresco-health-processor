package eu.xenit.alfresco.processor.modules;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContentExistenceValidatorTest {
    private final NodeRef nodeRef = new NodeRef("workspace://SpacesStore/a88561b3-c631-44cb-a883-180c6107a60b");
    private final String filterType = "{http://www.alfresco.org/model/content/1.0}content";
    private final QName folderType = QName.createQName("{http://www.alfresco.org/model/content/1.0}folder");
    private final QName filterTypeQName = QName.createQName(filterType);

    @Mock
    private ContentReader reader;

    @Mock
    private ContentService contentService;

    @Mock
    private NodeService nodeService;

    @Mock
    private DictionaryService dictionaryService;

    @Test
    public void nodeDoesNotExistTest() {
        when(nodeService.exists(any(NodeRef.class)))
                .thenReturn(false);
        boolean exists = new ContentExistenceValidator(filterType, contentService,
                nodeService, dictionaryService)
                .initialize()
                .validate(nodeRef);
        assertFalse(exists);
        verify(dictionaryService, never())
                .isSubClass(any(QName.class), any(QName.class));
        verify(contentService, never())
                .getReader(
                        any(NodeRef.class),
                        any(QName.class)
                );
        verify(reader, never()).exists();
    }

    @Test void nodeFilteredTest() {
        when(nodeService.exists(any(NodeRef.class)))
                .thenReturn(true);
        when(nodeService.getType(any(NodeRef.class)))
                .thenReturn(folderType);
        when(dictionaryService
                .isSubClass(any(QName.class), any(QName.class)))
                .thenReturn(false);
        boolean exists = new ContentExistenceValidator(filterType, contentService,
                nodeService, dictionaryService)
                .initialize()
                .validate(nodeRef);
        assertTrue(exists);
        verify(contentService, never())
                .getReader(
                        any(NodeRef.class),
                        any(QName.class)
                );
        verify(reader, never()).exists();
    }

    @Test
    public void validateContentExistenceTest() {
        when(nodeService.exists(any(NodeRef.class)))
                .thenReturn(true);
        when(nodeService.getType(any(NodeRef.class)))
                .thenReturn(filterTypeQName);
        when(dictionaryService
                .isSubClass(any(QName.class), any(QName.class)))
                .thenReturn(true);
        when(reader.exists()).thenReturn(true);
        when(
                contentService.getReader(
                        any(NodeRef.class),
                        any(QName.class)
                )
        ).thenReturn(reader);
        boolean exists = new ContentExistenceValidator(filterType, contentService,
                                nodeService, dictionaryService)
                            .initialize()
                            .validate(nodeRef);
        assertTrue(exists);
    }

    @Test()
    public void nullNodeRefThrowsTest() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            new ContentExistenceValidator(filterType, contentService,
                        nodeService, dictionaryService)
                    .validate(null);
        });
    }
}
