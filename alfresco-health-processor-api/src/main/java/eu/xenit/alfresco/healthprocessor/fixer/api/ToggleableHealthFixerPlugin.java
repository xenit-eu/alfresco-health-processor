package eu.xenit.alfresco.healthprocessor.fixer.api;

/**
 * {@link HealthFixerPlugin} that already has an <code>enabled</code> property
 *
 * @since 0.5.0
 */
public interface ToggleableHealthFixerPlugin extends HealthFixerPlugin {

    void setEnabled(boolean status);

}
