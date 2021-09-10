package eu.xenit.alfresco.healthprocessor.reporter.repository;

import lombok.AllArgsConstructor;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.beans.factory.InitializingBean;

@AllArgsConstructor
public class ReportTypeBehavior implements NodeServicePolicies.OnCreateNodePolicy, InitializingBean {

    private final NodeService nodeService;
    private final PolicyComponent policyComponent;


    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {
        nodeService.setProperty(childAssocRef.getChildRef(), ContentModel.PROP_IS_INDEXED, false);
        nodeService.setProperty(childAssocRef.getChildRef(), ContentModel.PROP_IS_CONTENT_INDEXED, false);
    }

    @Override
    public void afterPropertiesSet() {
        policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, HealthProcessorModel.TYPE_REPORT,
                new JavaBehaviour(this, "onCreateNode"));
        policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, HealthProcessorModel.TYPE_REPORTS,
                new JavaBehaviour(this, "onCreateNode"));
    }
}
