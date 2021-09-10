package eu.xenit.alfresco.healthprocessor.reporter.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;

@Slf4j
@AllArgsConstructor
public class DirectoryService {

    private NodeService nodeService;
    private DictionaryService dictionaryService;

    public NodeRef findChild(NodeRef parent, QNamePattern assocType, QNamePattern assocName) {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(parent, assocType, assocName);
        switch (children.size()) {
            case 0:
                throw new NoSuchNodeException(parent, assocType, assocName);
            case 1:
                return children.get(0).getChildRef();
            default:
                throw new DuplicateNodeException(parent, assocType, assocName);
        }
    }

    public NodeRef getOrCreateChild(NodeRef parent, QName assocType, QName assocName, QName newType) {
        try {
            return findChild(parent, assocType, assocName);
        } catch (NoSuchNodeException e) {
            log.info("Creating " + assocName + " folder in " + parent + " because it does not exist.");
            return nodeService.createNode(parent, assocType, assocName, newType).getChildRef();
        }
    }


    public NodeRef findChild(NodeRef parent, QName[] path) {
        NodeRef current = parent;
        try {
            for (QName pathPart : path) {
                current = findChild(current, RegexQNamePattern.MATCH_ALL, pathPart);
            }
        } catch (NoSuchNodeException noSuchNodeException) {
            throw new NoSuchNodeException(parent, path, noSuchNodeException);
        }
        return current;
    }

    private QName guessChildAssocType(NodeRef parent) {
        QName type = nodeService.getType(parent);
        Set<QName> aspects = nodeService.getAspects(parent);
        TypeDefinition mergedTypeDef = dictionaryService.getAnonymousType(type, aspects);
        Map<QName, ChildAssociationDefinition> childAssocDefs = mergedTypeDef.getChildAssociations();

        switch (childAssocDefs.size()) {
            case 0:
                throw new DictionaryException("Node " + parent + " (type: " + type + "; aspects: " + aspects
                        + ") does not allow child associations.");
            case 1:
                return childAssocDefs.keySet().iterator().next();
            default:
                throw new DictionaryException("Node " + parent + " (type: " + type + "; aspects: " + aspects
                        + ") has multiple possible child associations.");
        }

    }

    public NodeRef getOrCreateChild(NodeRef parent, QName[] path, QName containerType) {
        NodeRef current = parent;
        for (QName pathPart : path) {
            try {
                current = findChild(current, RegexQNamePattern.MATCH_ALL, pathPart);
            } catch (NoSuchNodeException noSuchNodeException) {
                QName childAssocType = guessChildAssocType(current);
                current = getOrCreateChild(current, childAssocType, pathPart, containerType);
                if (dictionaryService.getType(containerType).getProperties().containsKey(ContentModel.PROP_NAME)) {
                    nodeService.setProperty(current, ContentModel.PROP_NAME, pathPart.getLocalName());
                }
            }
        }
        return current;
    }

}
