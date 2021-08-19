package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import eu.xenit.alfresco.healthprocessor.plugins.solr.filter.FilteringSearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.plugins.solr.filter.NodeStoreFilter;
import eu.xenit.alfresco.healthprocessor.plugins.solr.filter.SolrNodeFilter;
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
            BeanDefinition nodeStoreFilter = createNodeStoreFilter(name);
            BeanDefinition searchSelector = createSelector(name, searchEndpoint);
            BeanDefinition searchSelectorFilter = createSelectorFilter(searchSelector, nodeStoreFilter);

            registry.registerBeanDefinition(SearchEndpointSelector.class.getName() + "#" + name, searchSelectorFilter);

        }
    }


    private BeanDefinition createSearchEndpointName(String name) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(SearchEndpoint.class);
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".base-uri}");

        return beanDefinition;
    }

    private BeanDefinition createSelector(String name, BeanDefinition searchEndpoint) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(
                "eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint.${" + PROPERTY_PREFIX + name
                        + ".type}SearchEndpointSelector");
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(searchEndpoint, SearchEndpoint.class.getName());
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".filter}");
        return beanDefinition;
    }

    private BeanDefinition createNodeStoreFilter(String name) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(NodeStoreFilter.class);
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".indexed-store}");
        return beanDefinition;
    }

    private BeanDefinition createSelectorFilter(BeanDefinition searchSelector, BeanDefinition nodeStoreFilter) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(FilteringSearchEndpointSelector.class);
        beanDefinition.setAutowireCandidate(true);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(searchSelector, SearchEndpointSelector.class.getName());
        constructorArgumentValues.addGenericArgumentValue(nodeStoreFilter, SolrNodeFilter.class.getName());
        return beanDefinition;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do here
    }
}
