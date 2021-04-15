package eu.xenit.alfresco.healthprocessor.util;

import static eu.xenit.alfresco.healthprocessor.util.AlfrescoAttributeStore.ATTR_KEY_HEALTH_PROCESSOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Map;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.attributes.AttributeService.AttributeQueryCallback;
import org.alfresco.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Stubber;

@ExtendWith(MockitoExtension.class)
class AlfrescoAttributeStoreTest {

    private static final String ATTR_KEY_1 = "key-1";
    private static final String ATTR_KEY_2_0 = "key-2.0";
    private static final String ATTR_KEY_2_1 = "key-2.1";
    private static final String ATTR_VALUE = "VALUE";

    @Mock
    private AttributeService attributeService;

    private AlfrescoAttributeStore attributeStore;

    @BeforeEach
    void setup() {
        attributeStore = new AlfrescoAttributeStore(attributeService);
    }

    @Test
    void getAttribute() {
        when(attributeService.getAttribute(ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, ATTR_KEY_2_0)).thenReturn(1L);

        assertThat(attributeStore.getAttribute(ATTR_KEY_1, ATTR_KEY_2_0), is(equalTo(1L)));
        assertThat(attributeStore.getAttribute(ATTR_KEY_1, "non-existing"), is(nullValue(Long.class)));
    }

    @Test
    void getAttribute_singleArgument() {
        when(attributeService.getAttribute(ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, null)).thenReturn(1L);

        assertThat(attributeStore.getAttribute(ATTR_KEY_1), is(equalTo(1L)));
        assertThat(attributeStore.getAttributeOrDefault(ATTR_KEY_1, 2L), is(equalTo(1L)));
        assertThat(attributeStore.getAttributeOrDefault("non-existing", 2L), is(equalTo(2L)));
    }

    @Test
    void getAttributes() {
        Stubber stubber = doAnswer(invocation -> {
            AttributeQueryCallback callback = invocation.getArgument(0);
            callback.handleAttribute(1L, "2.0", new String[]{ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, ATTR_KEY_2_0});
            callback.handleAttribute(2L, "2.1", new String[]{ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, ATTR_KEY_2_1});
            return null;
        });
        stubber.when(attributeService).getAttributes(any(), eq(ATTR_KEY_HEALTH_PROCESSOR), any());
        stubber.when(attributeService).getAttributes(any(), eq(ATTR_KEY_HEALTH_PROCESSOR));

        Map<Serializable, Serializable> attributes = attributeStore.getAttributes(ATTR_KEY_1);
        assertThat(attributes, is(notNullValue()));
        assertThat(attributes, hasEntry(ATTR_KEY_2_0, "2.0"));
        assertThat(attributes, hasEntry(ATTR_KEY_2_1, "2.1"));

        Map<Pair<Serializable, Serializable>, Serializable> allAttributes = attributeStore.getAllAttributes();
        assertThat(allAttributes, is(notNullValue()));
        assertThat(allAttributes, hasEntry(new Pair<>(ATTR_KEY_1, ATTR_KEY_2_0), "2.0"));
        assertThat(allAttributes, hasEntry(new Pair<>(ATTR_KEY_1, ATTR_KEY_2_1), "2.1"));
    }

    @Test
    void setAttribute() {
        attributeStore.setAttribute(ATTR_VALUE, ATTR_KEY_1);
        verify(attributeService).setAttribute(ATTR_VALUE, ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, null);

        attributeStore.setAttribute(ATTR_VALUE, ATTR_KEY_1, ATTR_KEY_2_0);
        verify(attributeService).setAttribute(ATTR_VALUE, ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, ATTR_KEY_2_0);
    }

    @Test
    void removeAttributes() {
        attributeStore.removeAttributes(ATTR_KEY_1);
        verify(attributeService).removeAttributes(ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1);

        attributeStore.removeAttributes(ATTR_KEY_1, ATTR_KEY_2_0);
        verify(attributeService).removeAttributes(ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_1, ATTR_KEY_2_0);
    }

    @Test
    void clearAttributes() {
        attributeStore.clearAttributes();
        verify(attributeService).removeAttributes(ATTR_KEY_HEALTH_PROCESSOR);
    }

}