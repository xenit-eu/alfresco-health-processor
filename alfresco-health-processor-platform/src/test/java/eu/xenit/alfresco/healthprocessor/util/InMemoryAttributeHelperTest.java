package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryAttributeHelperTest {

    private static final String ATTR_KEY_FOO = "foo";
    private static final String ATTR_VALUE_BAZ = "BAZINGA";

    private AttributeHelper attributeHelper;

    @BeforeEach
    void setup() {
        attributeHelper = new InMemoryAttributeHelper();
    }

    @Test
    void removeAttributes() {
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(nullValue()));
        attributeHelper.setAttribute(ATTR_VALUE_BAZ, ATTR_KEY_FOO);
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(equalTo(ATTR_VALUE_BAZ)));
        attributeHelper.removeAttributes(ATTR_KEY_FOO);
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(nullValue()));
    }

    @Test
    void clearAttributes() {
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(nullValue()));
        attributeHelper.setAttribute(ATTR_VALUE_BAZ, ATTR_KEY_FOO);
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(equalTo(ATTR_VALUE_BAZ)));
        attributeHelper.clearAttributes();
        assertThat(attributeHelper.getAttribute(ATTR_KEY_FOO), is(nullValue()));
    }

}
