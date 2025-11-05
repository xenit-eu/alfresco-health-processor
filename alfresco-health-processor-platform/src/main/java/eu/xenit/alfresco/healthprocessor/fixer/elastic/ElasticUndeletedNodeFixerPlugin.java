package eu.xenit.alfresco.healthprocessor.fixer.elastic;

import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;

/**
 * <p>
 * Interface representation of the {@link ElasticUndeletedNodeFixerPluginImpl} class.
 * </p>
 * <p>
 * The reason for tbe existence this interface is to allow the {@link org.alfresco.repo.management.subsystems.SubsystemProxyFactory}
 * to create a proxy of the {@link ElasticUndeletedNodeFixerPluginImpl} class from the health processor application context
 * in the main application context.
 * This proxy is only used as part of the integration tests from the health processor repo.
 * </p>
 */
public interface ElasticUndeletedNodeFixerPlugin extends ToggleableHealthFixerPlugin {

}