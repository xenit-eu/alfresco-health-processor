package eu.xenit.alfresco.healthprocessor.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.alfresco.util.Pair;

public class InMemoryAttributeStore implements AttributeStore {

    private final Map<Pair<Serializable, Serializable>, Serializable> attributes = new HashMap<>();

    @Override
    public <T> T getAttribute(Serializable key1, Serializable key2) {
        // noinspection unchecked
        return (T) attributes.get(new Pair<>(key1, key2));
    }

    @Override
    public Map<Pair<Serializable, Serializable>, Serializable> getAllAttributes() {
        return attributes;
    }

    @Override
    public Map<Serializable, Serializable> getAttributes(Serializable key1) {
        Map<Serializable, Serializable> ret = new HashMap<>();

        attributes.forEach((key, value) -> {
            if (key.getFirst().equals(key1)) {
                ret.put(key.getSecond(), value);
            }
        });

        return ret;
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
