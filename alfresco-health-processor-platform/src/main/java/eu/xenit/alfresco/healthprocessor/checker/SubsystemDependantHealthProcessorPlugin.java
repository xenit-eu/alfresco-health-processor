package eu.xenit.alfresco.healthprocessor.checker;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.checker.api.ToggleableHealthProcessorPlugin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public abstract class SubsystemDependantHealthProcessorPlugin extends ToggleableHealthProcessorPlugin
{

    @EqualsAndHashCode.Exclude
    private final SwitchableApplicationContextFactory searchApplicationContextFactory;

    protected final String subsystemName;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
    {
        boolean isEnabled = super.isEnabled();
        if (isEnabled && this.searchApplicationContextFactory != null)
        {
            isEnabled = subsystemName.equals(searchApplicationContextFactory.getCurrentSourceBeanName());
        }
        return isEnabled;
    }

}
