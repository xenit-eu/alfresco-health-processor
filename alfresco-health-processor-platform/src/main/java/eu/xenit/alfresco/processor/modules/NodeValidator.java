package eu.xenit.alfresco.processor.modules;

import org.alfresco.service.cmr.repository.NodeRef;

public interface NodeValidator {
    boolean validate(NodeRef nodeRef);
}
