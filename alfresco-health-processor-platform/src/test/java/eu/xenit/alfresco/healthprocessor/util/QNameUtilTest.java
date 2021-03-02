package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QNameUtilTest {

    private static final QName Q_NAME = QName.createQName("foobar", "baz");

    @Mock
    private NamespacePrefixResolver prefixResolver;

    @Test
    void toQName_null() {
        assertThrows(IllegalArgumentException.class, () -> QNameUtil.toQName(null, prefixResolver));
    }

    @Test
    void toQName() {
        assertThat(QNameUtil.toQName("{foobar}baz", prefixResolver), is(equalTo(Q_NAME)));
        verifyNoMoreInteractions(prefixResolver);
    }

    @Test
    void toQName_usingNamespaceService() {
        when(prefixResolver.getNamespaceURI("fb")).thenReturn("foobar");
        assertThat(QNameUtil.toQName("fb:baz", prefixResolver), is(equalTo(Q_NAME)));
        verify(prefixResolver).getNamespaceURI("fb");
    }

    @Test
    void toQNames() {
        assertThat(QNameUtil.toQNames(Collections.singleton("{foobar}baz"), prefixResolver), contains(Q_NAME));
    }

    @Test
    void toQNames_emptyValuesFiltered() {
        assertThat(QNameUtil.toQNames(Arrays.asList("{foobar}baz", " "), prefixResolver), contains(Q_NAME));
    }

}