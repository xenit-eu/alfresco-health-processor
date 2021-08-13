package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AbstractFactoryBean;

@Slf4j
@AllArgsConstructor
public final class SolrServerEndpointSelectorFactoryBean extends AbstractFactoryBean<SolrServerEndpointSelector> {

    private Properties globalProperties;

    private String PROPERTY_PREFIX = "eu.xenit.alfresco.healthprocessor.plugin.solr-index.endpoints";

    @Override
    public Class<?> getObjectType() {
        return SolrServerEndpointSelector.class;
    }

    @Override
    protected SolrServerEndpointSelector createInstance() throws Exception {
        return new AggregateEndpointSelector(createSelectors());
    }

    private Set<SolrServerEndpointSelector> createSelectors() {
        String[] endpointNames = globalProperties.getProperty(PROPERTY_PREFIX).split(",");

        return Arrays.stream(endpointNames)
                .map(this::createSelector)
                .collect(Collectors.toSet());
    }

    private SolrServerEndpointSelector createSelector(String selector) {
        String prefix = PROPERTY_PREFIX + "." + selector + ".";

        String type = globalProperties.getProperty(prefix + "type");

        SolrEndpointType solrEndpointType = SolrEndpointType.fromType(type);

        String filter = globalProperties.getProperty(prefix + "filter", null);

        SearchEndpoint endpoint = createSearchEndpoint(selector);

        log.info("Created endpoint {} for {} with filter {}:{}", selector, endpoint, solrEndpointType, filter);

        return solrEndpointType.createFromFilter(filter, endpoint);
    }

    private SearchEndpoint createSearchEndpoint(String selector) {
        String prefix = PROPERTY_PREFIX + "." + selector + ".";

        return new SearchEndpoint(
                globalProperties.getProperty(prefix + "host"),
                Integer.parseInt(globalProperties.getProperty(prefix + "port", "8080"), 10),
                globalProperties.getProperty(prefix + "prefix", "solr"),
                globalProperties.getProperty(prefix + "core", "alfresco")
        );
    }
}
