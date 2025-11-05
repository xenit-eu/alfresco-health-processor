package eu.xenit.alfresco.healthprocessor.fixer.elastic;

import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;

import eu.xenit.alfresco.healthprocessor.endpoint.elastic.ElasticEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.ElasticRequestExecutor;
import eu.xenit.alfresco.healthprocessor.fixer.SubsystemDependantHealthFixerPlugin;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public abstract class AbstractElasticFixerPlugin extends SubsystemDependantHealthFixerPlugin<ElasticEndpoint>
{

    @EqualsAndHashCode.Exclude
    protected final ElasticRequestExecutor elasticRequestExecutor;

    public AbstractElasticFixerPlugin(SwitchableApplicationContextFactory searchApplicationContextFactory, String subsystemName,
            ElasticRequestExecutor elasticRequestExecutor)
    {
        super(ElasticEndpoint.class, searchApplicationContextFactory, subsystemName);
        this.elasticRequestExecutor = elasticRequestExecutor;
    }

}
