package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;


class Alfresco7TrackingComponentBeanPostProcessorTest {

    @Test
    void postProcessBeanDefinitionRegistry_SearchTrackingComponent() {
        TrackingComponentBeanPostProcessor postProcessor = new TrackingComponentBeanPostProcessor();
        BeanDefinitionRegistry beanDefinitionRegistry = mock(BeanDefinitionRegistry.class);
        postProcessor.postProcessBeanDefinitionRegistry(beanDefinitionRegistry);

        ArgumentCaptor<BeanDefinition> argument = ArgumentCaptor.forClass(BeanDefinition.class);
        verify(beanDefinitionRegistry).registerBeanDefinition(
                eq(TrackingComponentBeanPostProcessor.TRACKING_COMPONENT_BEAN_ID),
                argument.capture());

        assertThat(argument.getValue(), is(not(nullValue())));
        assertThat(argument.getValue().getBeanClassName(), is(Alfresco7TrackingComponent.class.getCanonicalName()));
        assertThat(
                ((RuntimeBeanReference) argument.getValue()
                        .getConstructorArgumentValues()
                        .getGenericArgumentValue(RuntimeBeanReference.class).getValue())
                        .getBeanName(),
                is("searchTrackingComponent"));

    }

}