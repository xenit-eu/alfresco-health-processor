package eu.xenit.alfresco.healthprocessor.reporter.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

@AllArgsConstructor
public class DetailsAspectBehavior implements NodeServicePolicies.OnUpdatePropertiesPolicy,
        InitializingBean {

    private final NodeService nodeService;
    private final PolicyComponent policyComponent;

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        ifModified(HealthProcessorModel.PROP_FIXED_NODES, NodeRef.class, before, after, fixedNodes -> {
            if (fixedNodes == null) {
                nodeService.setProperty(nodeRef, HealthProcessorModel.PROP_FIXED_NODE_COUNT, 0);
            } else {
                nodeService.setProperty(nodeRef, HealthProcessorModel.PROP_FIXED_NODE_COUNT, fixedNodes.size());
            }
        });
        ifModified(HealthProcessorModel.PROP_UNHEALTHY_NODES, NodeRef.class, before, after, fixedNodes -> {
            if (fixedNodes == null) {
                nodeService.setProperty(nodeRef, HealthProcessorModel.PROP_UNHEALTHY_NODE_COUNT, 0);
            } else {
                nodeService.setProperty(nodeRef, HealthProcessorModel.PROP_UNHEALTHY_NODE_COUNT, fixedNodes.size());
            }
        });
    }

    private <T> void ifModified(QName property, Class<T> type, Map<QName, Serializable> before,
            Map<QName, Serializable> after, Consumer<Collection<T>> update) {
        Collection<T> beforeList = DefaultTypeConverter.INSTANCE.getCollection(type,
                before.getOrDefault(property, new ArrayList<>()));
        Collection<T> afterList = DefaultTypeConverter.INSTANCE.getCollection(type,
                after.getOrDefault(property, new ArrayList<>()));

        if (!Objects.equals(beforeList, afterList)) {
            update.accept(afterList);
        }
    }

    @Override
    public void afterPropertiesSet() {
        policyComponent.bindPropertyBehaviour(OnUpdatePropertiesPolicy.QNAME, HealthProcessorModel.ASPECT_DETAILS,
                new JavaBehaviour(this, "onUpdateProperties"));
    }
}
