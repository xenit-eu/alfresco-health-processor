package eu.xenit.alfresco.healthprocessor.reporter.repository;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QNamePattern;

public class NoSuchNodeException extends AlfrescoRuntimeException {

    public NoSuchNodeException(NodeRef parent, QNamePattern typeQNamePattern, QNamePattern qNamePattern) {
        super("Node " + parent + " does not have a child with association type " + typeQNamePattern + " and name "
                + qNamePattern);
    }

}
