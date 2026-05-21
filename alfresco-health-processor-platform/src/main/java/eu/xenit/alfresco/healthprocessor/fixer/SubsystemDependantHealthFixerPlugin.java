package eu.xenit.alfresco.healthprocessor.fixer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.checker.NodeIndexHealthReport;
import eu.xenit.alfresco.healthprocessor.checker.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.ToggleableHealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class SubsystemDependantHealthFixerPlugin<T extends SearchEndpoint> implements ToggleableHealthFixerPlugin
{

    protected final Class<T> endpointClass;

    @EqualsAndHashCode.Exclude
    private final SwitchableApplicationContextFactory searchApplicationContextFactory;

    protected final String subsystemName;
    
    private @Setter boolean enabled;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
    {
        boolean isEnabled = this.enabled;
        if (isEnabled && this.searchApplicationContextFactory != null)
        {
            isEnabled = subsystemName.equals(searchApplicationContextFactory.getCurrentSourceBeanName());
        }
        return isEnabled;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Set<NodeFixReport> fix(Class<? extends HealthProcessorPlugin> pluginClass, Set<NodeHealthReport> unhealthyReports)
    {
        Set<NodeFixReport> fixReports = new HashSet<>();
        for (NodeHealthReport unhealthyReport : unhealthyReports)
        {
            Iterator<NodeIndexHealthReport<T>> it = unhealthyReport.data(NodeIndexHealthReport.class).stream()
                    .filter(hr -> endpointClass.isInstance(hr.getEndpoint())).map(hr -> (NodeIndexHealthReport<T>) hr)
                    .iterator();
            while (it.hasNext())
            {
                fixReports.addAll(handleHealthReport(unhealthyReport, it.next()));
            }
        }

        return fixReports;
    }

    protected abstract Set<NodeFixReport> handleHealthReport(NodeHealthReport unhealthyReport,
            NodeIndexHealthReport<T> endpointHealthReport);
}
