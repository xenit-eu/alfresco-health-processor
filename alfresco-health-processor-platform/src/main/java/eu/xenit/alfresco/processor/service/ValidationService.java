package eu.xenit.alfresco.processor.service;

import eu.xenit.alfresco.processor.modules.NodeValidator;
import lombok.AllArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.List;

@AllArgsConstructor
public class ValidationService {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    protected final List<NodeValidator> nodeValidators;

    public void validate(List<NodeRef> nodeRefs) {
        if(nodeRefs == null || nodeRefs.isEmpty()) {
            throw new InvalidParameterException("NodeRefs list cannot be null or empty!");
        }
        if(nodeValidators == null || nodeValidators.isEmpty()) {
            logger.warn("No validators provided, aborting!");
            return;
        }
        for(NodeRef nodeRef : nodeRefs) {
            for (NodeValidator nodeValidator : nodeValidators) {
                if(!nodeValidator.validate(nodeRef)) {
                    // TODO Throw an exception ?
                    logger.warn("NodeRef {} is in invalid state!", nodeRef);
                    break;
                }
            }
        }
    }
}
