package eu.xenit.alfresco.healthprocessor.endpoint.elastic;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;

import eu.xenit.alfresco.healthprocessor.endpoint.AggregateSearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.AlwaysSearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers beans ({@link SolrEndpoint} and {@link SearchEndpointSelector}) for every configured endpoint.
 */
@Slf4j
@AllArgsConstructor
public class ElasticEndpointSelectorBeanPostProcessor implements BeanDefinitionRegistryPostProcessor
{

    private Properties globalProperties;

    private static final String ENDPOINTS_PROPERTY = "eu.xenit.alfresco.healthprocessor.checker.elastic-index.endpoints";

    private static final String PROPERTY_PREFIX = ENDPOINTS_PROPERTY + ".";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException
    {
        String[] endpointNames = globalProperties.getProperty(ENDPOINTS_PROPERTY, "default").split(",");
        if (endpointNames.length == 1 && endpointNames[0].isEmpty())
        {
            log.warn("ElasticSearch index validation plugin has no endpoints are configured. No endpoints will be checked.");
            return;
        }

        BeanDefinition aggregateSelector = registry.getBeanDefinition(
                "eu.xenit.alfresco.healthprocessor.endpoint.elastic." + AggregateSearchEndpointSelector.class.getSimpleName());
        ManagedList<RuntimeBeanReference> selectors = new ManagedList<>();
        selectors.setElementTypeName(SearchEndpointSelector.class.getName());
        for (String name : endpointNames)
        {
            log.info("Registering beans for ElasticSearch endpoint {}", name);
            BeanDefinition searchEndpoint = createSearchEndpointName(name);
            BeanDefinition searchSelector = createSelector(name, searchEndpoint);

            String beanName = SearchEndpointSelector.class.getName() + "#elastic#" + name;
            registry.registerBeanDefinition(beanName, searchSelector);
            selectors.add(new RuntimeBeanReference(beanName));
        }
        ConstructorArgumentValues constructorArgumentValues = aggregateSelector.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(selectors);
    }

    private BeanDefinition createSearchEndpointName(String name)
    {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ElasticEndpoint.class);
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "${" + PROPERTY_PREFIX + name + ".secureComms}");
        constructorArgumentValues.addIndexedArgumentValue(1, "${" + PROPERTY_PREFIX + name + ".base-uri}");
        constructorArgumentValues.addIndexedArgumentValue(2, "${" + PROPERTY_PREFIX + name + ".expect-paths-indexed}");

        return beanDefinition;
    }

    private BeanDefinition createSelector(String name, BeanDefinition searchEndpoint)
    {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(AlwaysSearchEndpointSelector.class);
        beanDefinition.setAutowireCandidate(false);
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(searchEndpoint, SearchEndpoint.class.getName());
        constructorArgumentValues.addIndexedArgumentValue(0, "Always");
        return beanDefinition;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // nothing to do here
    }
}
