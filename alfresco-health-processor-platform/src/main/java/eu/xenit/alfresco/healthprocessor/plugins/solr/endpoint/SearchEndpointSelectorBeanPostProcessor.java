package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * Registers beans ({@link SearchEndpoint} and {@link SearchEndpointSelector}) for every configured endpoint.
 */
@Slf4j
@AllArgsConstructor
public class SearchEndpointSelectorBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private Properties globalProperties;

    private static final String ENDPOINTS_PROPERTY = "eu.xenit.alfresco.healthprocessor.plugin.solr-index.endpoints";
    private static final String PROPERTY_PREFIX = ENDPOINTS_PROPERTY + ".";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] endpointNames = globalProperties.getProperty(ENDPOINTS_PROPERTY, "default,archive").split(",");
        if (endpointNames.length == 1 && endpointNames[0].isEmpty()) {
            log.warn("Solr index validation plugin has no endpoints are configured. No endpoints will be checked.");
            return;
        }
        for (String name : endpointNames) {
            log.info("Registering beans for solr endpoint {}", name);
            BeanDefinition searchEndpoint = createSearchEndpointName(name);
            registry.registerBeanDefinition(SearchEndpoint.class.getName()+"#"+name, searchEndpoint);
            BeanDefinition searchSelector = createSelector(name, searchEndpoint);
            registry.registerBeanDefinition(SearchEndpointSelector.class.getName()+"#"+name, searchSelector);
        }
    }

    private BeanDefinition createSearchEndpointName(String name) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(SearchEndpoint.class);
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".base-uri}");
        constructorArgumentValues.addIndexedArgumentValue(1, "${"+PROPERTY_PREFIX+name+".indexed-store}");

        return beanDefinition;
    }

    private BeanDefinition createSelector(String name, BeanDefinition searchEndpoint) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName("eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.${"+PROPERTY_PREFIX+name+".type}SearchEndpointSelector");
        beanDefinition.setAutowireCandidate(true);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(searchEndpoint, SearchEndpoint.class.getName());
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".filter}");
        return beanDefinition;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do here
    }
}
