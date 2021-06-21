package eu.xenit.alfresco.healthprocessor.plugins.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

@RequiredArgsConstructor
public abstract class FilteredSingleNodeHealthProcessorPlugin extends SingleNodeHealthProcessorPlugin {

    protected final NodeService nodeService;
    private final DictionaryService dictionaryService;

    private boolean nodeShouldExist = true;
    private List<StoreRef> storeRefWhiteList = Collections.singletonList(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
    private List<QName> nodeTypeWhiteList = Collections.emptyList();
    private List<QName> nodeTypeBlackList = Collections.emptyList();
    private List<QName> aspectsWhiteList = Collections.emptyList();
    private List<QName> aspectsBlackList = Collections.emptyList();
    private List<QName> propertiesWhiteList = Collections.emptyList();
    private List<QName> propertiesBlackList = Collections.emptyList();

    @Override
    protected final NodeHealthReport process(NodeRef nodeRef) {
        if (nodeShouldExist && !nodeService.exists(nodeRef) || nodeService.getNodeStatus(nodeRef).isDeleted()) {
            getLogger().debug("Skipping node '{}'. Node does not exist or is deleted", nodeRef);
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
        }

        if (!storeRefWhiteList.contains(nodeRef.getStoreRef())) {
            getLogger()
                    .debug("Skipping node '{}'. StoreRef is not in whitelisted refs ({})", nodeRef, storeRefWhiteList);
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
        }

        final QName nodeType = nodeService.getType(nodeRef);
        if (!nodeTypeWhiteList.isEmpty() && !isQNameInList(nodeType, nodeTypeWhiteList)) {
            getLogger().debug("Skipping node '{}'. Type not in whitelist ({})", nodeRef,
                    nodeTypeWhiteList);
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
        }
        if (!nodeTypeBlackList.isEmpty() && isQNameInList(nodeType, nodeTypeBlackList)) {
            getLogger().debug("Skipping node '{}'. Type in blacklist ({})", nodeRef,
                    nodeTypeWhiteList);
            return new NodeHealthReport(NodeHealthStatus.NONE, nodeRef);
        }

        return doProcess(nodeRef);
    }

    protected abstract NodeHealthReport doProcess(NodeRef nodeRef);

    private boolean isQNameInList(final QName qNameToCheck, final List<QName> qNames) {
        for (QName qName : qNames) {
            if (isClassOrSubClass(qNameToCheck, qName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassOrSubClass(final QName qNameToCheck, final QName clazzOrSuperClazz) {
        if (qNameToCheck.equals(clazzOrSuperClazz)) {
            return true;
        }
        return dictionaryService.isSubClass(qNameToCheck, clazzOrSuperClazz);
    }


}
