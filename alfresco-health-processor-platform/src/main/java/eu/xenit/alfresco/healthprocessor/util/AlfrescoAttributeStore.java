package eu.xenit.alfresco.healthprocessor.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.util.Pair;

@RequiredArgsConstructor
public class AlfrescoAttributeStore implements AttributeStore {

    static final String ATTR_KEY_HEALTH_PROCESSOR = "health-processor";

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
    public Map<Serializable, Serializable> getAttributes(Serializable key) {
        Map<Serializable, Serializable> ret = new HashMap<>();

        attributeService.getAttributes((id, value, keys) -> {
            ret.put(keys[2], value);
            return true;
        }, ATTR_KEY_HEALTH_PROCESSOR, key);

        return ret;
    }

    @Override
    public Map<Pair<Serializable, Serializable>, Serializable> getAllAttributes() {
        Map<Pair<Serializable, Serializable>, Serializable> ret = new HashMap<>();

        attributeService.getAttributes((id, value, keys) -> {
            ret.put(new Pair<>(keys[1], keys[2]), value);
            return true;
        }, ATTR_KEY_HEALTH_PROCESSOR);

        return ret;
    }

    @Override
    public void setAttribute(Serializable value, Serializable key1, Serializable key2) {
        attributeService.setAttribute(value, ATTR_KEY_HEALTH_PROCESSOR, key1, key2);
    }

    @Override
    public void removeAttributes(Serializable key1, Serializable key2) {
        if (key1 == null && key2 == null) {
            attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR);
        } else if (key2 == null) {
            attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR, key1);
        } else {
            attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR, key1, key2);
        }
    }

    @Override
    public void clearAttributes() {
        removeAttributes(null);
    }
}
