package eu.xenit.alfresco.healthprocessor.util;

import java.io.Serializable;

public interface AttributeHelper {

    String KEY_STATE = "state";

    default <T> T getAttribute(Serializable key1) {
        return getAttribute(key1, null);
    }

    default <T> T getAttributeOrDefault(Serializable key1, T defaultValue) {
        T ret = getAttribute(key1);
        return (ret == null) ? defaultValue : ret;
    }

    <T> T getAttribute(Serializable key1, Serializable key2);

    default void setAttribute(Serializable value, Serializable key1) {
        setAttribute(value, key1, null);
    }

    void setAttribute(Serializable value, Serializable key1, Serializable key2);

    default void removeAttributes(Serializable key1) {
        this.removeAttributes(key1, null);
    }

    void removeAttributes(Serializable key1, Serializable key2);

    void clearAttributes();

}
