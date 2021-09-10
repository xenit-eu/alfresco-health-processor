package eu.xenit.alfresco.healthprocessor.reporter.repository;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QNamePattern;

public class DuplicateNodeException extends AlfrescoRuntimeException {

    public DuplicateNodeException(NodeRef parent, QNamePattern typeQNamePattern, QNamePattern qNamePattern) {
        super("Node " + parent + " has multiple children with association type " + typeQNamePattern + " and name "
                + qNamePattern);
    }

}
