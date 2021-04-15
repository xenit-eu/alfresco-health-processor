package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryAttributeStoreTest {

    private static final String ATTR_KEY_FOO = "foo";
    private static final String ATTR_VALUE_BAZ = "BAZINGA";

    private AttributeStore attributeStore;

    @BeforeEach
    void setup() {
        attributeStore = new InMemoryAttributeStore();
    }

    @Test
    void removeAttributes() {
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(nullValue()));
        attributeStore.setAttribute(ATTR_VALUE_BAZ, ATTR_KEY_FOO);
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(equalTo(ATTR_VALUE_BAZ)));
        attributeStore.removeAttributes(ATTR_KEY_FOO);
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(nullValue()));
    }

    @Test
    void clearAttributes() {
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(nullValue()));
        attributeStore.setAttribute(ATTR_VALUE_BAZ, ATTR_KEY_FOO);
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(equalTo(ATTR_VALUE_BAZ)));
        attributeStore.clearAttributes();
        assertThat(attributeStore.getAttribute(ATTR_KEY_FOO), is(nullValue()));
    }

}
