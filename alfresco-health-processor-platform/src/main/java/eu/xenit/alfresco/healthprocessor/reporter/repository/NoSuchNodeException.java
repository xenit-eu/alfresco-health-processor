package eu.xenit.alfresco.healthprocessor.reporter.repository;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

public class NoSuchNodeException extends AlfrescoRuntimeException {

    public NoSuchNodeException(NodeRef parent, QNamePattern typeQNamePattern, QNamePattern qNamePattern) {
        super("Node " + parent + " does not have a child with association type " + typeQNamePattern + " and name "
                + qNamePattern);
    }

    public NoSuchNodeException(NodeRef parent, QName[] path, Throwable throwable) {
        super("Node " + parent + " does not have child with path " + Arrays.stream(path).map(QName::toPrefixString)
                .collect(Collectors.joining("/")), throwable);
    }


}
