package eu.xenit.alfresco.healthprocessor.indexing;

import static eu.xenit.alfresco.healthprocessor.indexing.AlfrescoTrackingComponentFactoryBean.SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7;
import static eu.xenit.alfresco.healthprocessor.indexing.AlfrescoTrackingComponentFactoryBean.SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.alfresco.repo.solr.SOLRTrackingComponentImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AlfrescoTrackingComponentFactoryBeanTest {

    @Mock
    private ApplicationContext parentContext;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SOLRTrackingComponentImpl solrTrackingComponent;

    private AlfrescoTrackingComponentFactoryBean factoryBean;

    @BeforeEach
    void setup() {
        factoryBean = new AlfrescoTrackingComponentFactoryBean();
        when(applicationContext.getParent()).thenReturn(parentContext);
        when(parentContext.getParent()).thenReturn(null);
        factoryBean.setApplicationContext(applicationContext);
    }

    @Test
    void getObjectType() {
        assertThat(factoryBean.getObjectType(), is(equalTo(AlfrescoTrackingComponent.class)));
    }

    @Test
    void createInstance_beanNotAvailable() {
        assertThrows(IllegalStateException.class, () -> factoryBean.createInstance());
    }

    @Test
    void createInstance_invalidType() {
        when(parentContext.containsBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7))
                .thenReturn(true);
        when(parentContext.getBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7)).thenReturn("I'm a String");

        assertThrows(ClassCastException.class, () -> factoryBean.createInstance());
    }

    @Test
    void createInstance_preAlfresco7BeanAvailable() {
        when(parentContext.containsBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7))
                .thenReturn(true);
        when(parentContext.getBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7))
                .thenReturn(solrTrackingComponent);

        assertThat(factoryBean.createInstance().getSolrTrackingComponent(), sameInstance(solrTrackingComponent));
    }

    @Test
    void createInstance_postAlfresco7BeanAvailable() {
        when(parentContext.containsBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7))
                .thenReturn(true);
        when(parentContext.getBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7))
                .thenReturn(solrTrackingComponent);

        assertThat(factoryBean.createInstance().getSolrTrackingComponent(), sameInstance(solrTrackingComponent));
    }

}