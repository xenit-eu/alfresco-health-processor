package eu.xenit.alfresco.healthprocessor.util;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.attributes.AttributeService;

@RequiredArgsConstructor
public class AlfrescoAttributeHelper implements AttributeHelper {

    public static final String ATTR_KEY_HEALTH_PROCESSOR = "health-processor";

    private final AttributeService attributeService;

    @Override
    public <T> T getAttribute(Serializable key1, Serializable key2) {
        Serializable ret = attributeService.getAttribute(ATTR_KEY_HEALTH_PROCESSOR, key1, key2);
        if (ret == null) {
            return null;
        }
        // noinspection unchecked
        return (T) ret;
    }

    @Override
    public void setAttribute(Serializable value, Serializable key1, Serializable key2) {
        attributeService.setAttribute(value, ATTR_KEY_HEALTH_PROCESSOR, key1, key2);
    }

    @Override
    public void removeAttributes(Serializable key1, Serializable key2) {
        attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR, key1, key2);
    }

    public void clearAttributes() {
        attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR);
    }
}
