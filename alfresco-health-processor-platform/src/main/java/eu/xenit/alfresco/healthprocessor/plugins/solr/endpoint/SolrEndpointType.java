package eu.xenit.alfresco.healthprocessor.plugins.solr.endpoint;

import javax.annotation.Nonnull;

public enum SolrEndpointType {
    ALWAYS((filter, searchEndpoint) -> new AlwaysSolrServerEndpointSelector(searchEndpoint)),
    DB_ID_RANGE(DbIdRangeSolrServerEndpointSelector::new);

    @FunctionalInterface
    private interface SolrServerEndpointSelectorFactory {

        @Nonnull
        SolrServerEndpointSelector create(String filter, SearchEndpoint searchEndpoint);
    }

    private final SolrServerEndpointSelectorFactory factory;

    SolrEndpointType(SolrServerEndpointSelectorFactory factory) {
        this.factory = factory;
    }

    public static SolrEndpointType fromType(String key) {
        for (SolrEndpointType s : values()) {
            if (s.name().equalsIgnoreCase(key)) {
                return s;
            }
        }
        return null;
    }

    @Nonnull
    public SolrServerEndpointSelector createFromFilter(String filter, SearchEndpoint searchEndpoint) {
        return factory.create(filter, searchEndpoint);
    }
}
