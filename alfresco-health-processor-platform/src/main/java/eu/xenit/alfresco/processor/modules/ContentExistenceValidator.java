package eu.xenit.alfresco.processor.modules;

import lombok.RequiredArgsConstructor;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;

@RequiredArgsConstructor
public class ContentExistenceValidator implements NodeValidator {
    private static final Logger logger = LoggerFactory.getLogger(ContentExistenceValidator.class);

    private QName filterTypeQName = null;

    protected final String filterType;

    protected final ContentService contentService;

    protected final NodeService nodeService;

    protected final DictionaryService dictionaryService;

    ContentExistenceValidator initialize() {
        filterTypeQName = QName.createQName(filterType);
        return this;
    }

    @Override
    public boolean validate(NodeRef nodeRef) {
        logger.trace(">>> Entering ContentExistenceValidator.validate()");
        if(nodeRef == null) {
            throw new InvalidParameterException("NodeRef cannot be null!");
        }

        if(!this.nodeService.exists(nodeRef)) {
            logger.debug("Node does not exist: {}", nodeRef);
            return false;
        }

        QName type = this.nodeService.getType(nodeRef);
        if(!this.dictionaryService.isSubClass(type, filterTypeQName)) {
            logger.debug("Node {} is of type {}, skipping because it's subclass of {}", nodeRef, type, filterTypeQName);
            return true;
        }

        ContentReader reader = this.contentService
                .getReader(nodeRef, ContentModel.PROP_CONTENT);

        if(reader != null && reader.exists()) {
            logger.debug("Node Content Does Exist {}", nodeRef);
            logger.trace("<<< Exiting ContentExistenceValidator.validate()");
            return true;
        }

        logger.error("Node Content Does Not Exist {}", nodeRef);
        return false;
    }
}
