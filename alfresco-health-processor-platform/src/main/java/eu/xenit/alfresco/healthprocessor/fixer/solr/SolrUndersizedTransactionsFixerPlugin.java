package eu.xenit.alfresco.healthprocessor.fixer.solr;

import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;

/**
 * <p>
 * Interface representation of the {@link SolrMissingNodeFixerPluginImpl} class.
 * </p>
 * <p>
 * The reason for tbe existence this interface is to allow the {@link org.alfresco.repo.management.subsystems.SubsystemProxyFactory}
 * to create a proxy of the {@link SolrUndersizedTransactionsFixerPluginImpl} class from the health processor application context
 * in the main application context.
 * This proxy is only used as part of the integration tests from the health processor repo.
 * </p>
 */
public interface SolrUndersizedTransactionsFixerPlugin extends ToggleableHealthFixerPlugin {

}
