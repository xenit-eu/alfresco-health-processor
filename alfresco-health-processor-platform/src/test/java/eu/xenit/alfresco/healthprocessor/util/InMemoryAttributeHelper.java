package eu.xenit.alfresco.healthprocessor.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.util.Pair;

public class InMemoryAttributeHelper implements AttributeHelper {

    private final Map<Pair<Serializable, Serializable>, Serializable> attributes = new HashMap<>();

    @Override
    public <T> T getAttribute(Serializable key1, Serializable key2) {
        // noinspection unchecked
        return (T) attributes.get(new Pair<>(key1, key2));
    }

    @Override
    public void setAttribute(Serializable value, Serializable key1, Serializable key2) {
        attributes.put(new Pair<>(key1, key2), value);
    }

    @Override
    public void removeAttributes(Serializable key1, Serializable key2) {
        attributes.remove(new Pair<>(key1, key2));
    }

    @Override
    public void clearAttributes() {
        attributes.clear();
    }
}
