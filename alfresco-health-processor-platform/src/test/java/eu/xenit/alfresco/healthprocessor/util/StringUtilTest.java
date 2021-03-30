package eu.xenit.alfresco.healthprocessor.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashSet;
import java.util.Set;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    void join() {
        Set<QName> qnames = new HashSet<>();

        assertThat(StringUtil.join(null, ", "), is(nullValue()));
        assertThat(StringUtil.join(qnames, ", "), is(equalTo("")));

        qnames.add(ContentModel.PROP_TITLE);
        assertThat(StringUtil.join(qnames, ", "), is(equalTo("{http://www.alfresco.org/model/content/1.0}title")));

        qnames.add(ContentModel.PROP_NAME);
        assertThat(StringUtil.join(qnames, ", "), is(equalTo(
                "{http://www.alfresco.org/model/content/1.0}name, {http://www.alfresco.org/model/content/1.0}title")));

    }

}