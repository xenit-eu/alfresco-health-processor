package eu.xenit.alfresco.processor.service;

import lombok.AllArgsConstructor;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.attributes.DuplicateAttributeException;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
/*
 * {@link AttributeService} wrapper that allows different Alfresco nodes to claim ranges of transactions.
 */
public class ProcessorAttributeService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorAttributeService.class);

    public static final String ATTR_KEY_HEALTH_PROCESSOR = "HealthProcessor";
    public static final String ATTR_KEY_IS_RUNNING = "IsRunning";

    private static final int EXPECTED_ATTRIBUTE_KEYS_LENGTH = 3;
    private static final int ATTRIBUTE_BASE_KEY_IDX = 0;
    private static final int ATTRIBUTE_KEY_IDX = 1;
    private static final int ATTRIBUTE_VALUE_IDX = 1;

    protected final AttributeService attributeService;
    protected final DescriptorService descriptorService;

    /**
     *
     * @param key Attribute Key to be read
     * @param defaultValue Value to return in case the attribute does not exist
     * @return Returns persisted value for the specified attribute. Any value != TRUE will return false.
     */
    public boolean getAttribute(final String key, final boolean defaultValue) {
        final List<Boolean> returnValues = new ArrayList<>();

        if(!attributeService.exists(ATTR_KEY_HEALTH_PROCESSOR,key)) {
            return defaultValue;
        }

        attributeService.getAttributes((Long id, Serializable value, Serializable[] keys) -> {
            logger.debug("Processing attribute id: '{}', keys: '{}', value: '{}'", id, keys, value);
            returnValues.add(extractAttributeValue(key, keys));
            return true;
        }, ATTR_KEY_HEALTH_PROCESSOR, key);

        Optional<Boolean> firstResult = returnValues.stream().findFirst();
        return firstResult.isPresent() && firstResult.get();
    }

    public void persistAttribute(final String key, final boolean value) {
        try {
            attributeService.createAttribute(getRepositoryId(), ATTR_KEY_HEALTH_PROCESSOR, key, value);
        } catch (DuplicateAttributeException e) {
            logger.info("Failed to persist attribute '{}'", value);
            logger.debug("Cause", e);
        }
    }

    public void cleanupAttributes() {
        attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR);
    }

    String getRepositoryId() {
        return descriptorService.getCurrentRepositoryDescriptor().getId();
    }

    /**
     * @param key Attribute Key to be read
     * @param attributeKeys Attribute Key collection to be scanned
     * @return Any value != TRUE will return false.
     */
    static boolean extractAttributeValue(final String key, Serializable[] attributeKeys){
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        validateKeyLength(key, attributeKeys);
        validateBaseAttributeKeyIsHealthProcessor(key, attributeKeys);
        validateAttributeKey(key, attributeKeys);
        return Boolean.parseBoolean(attributeKeys[ATTRIBUTE_VALUE_IDX].toString());
    }

    static String toString(Serializable[] attributeKeys) {
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        return String.join(",",
                Arrays.stream(attributeKeys)
                            .map(Object::toString)
                            .collect(Collectors.toSet()));
    }

    private static void validateKeyLength(String key, Serializable[] attributeKeys) {
        if (attributeKeys.length == EXPECTED_ATTRIBUTE_KEYS_LENGTH) {
            return;
        }
        
        final String message = "Expected attribute key length: 3 ('"
                + ATTR_KEY_HEALTH_PROCESSOR + "." + key + ".[true|false]')"
                + "but was: " + attributeKeys.length + "('" + toString(attributeKeys) + ")'";
        throw new IllegalArgumentException(message);
    }

    private static void validateBaseAttributeKeyIsHealthProcessor(String key, Serializable[] attributeKeys) {
        if (ATTR_KEY_HEALTH_PROCESSOR.equals(attributeKeys[ATTRIBUTE_BASE_KEY_IDX])) {
            return;
        }

        final String message = "Expected first attribute key to be: '"
                + ATTR_KEY_HEALTH_PROCESSOR
                + "but was: '" + attributeKeys[ATTRIBUTE_BASE_KEY_IDX] + "'";
        throw new IllegalArgumentException(message);
    }

    private static void validateAttributeKey(String key, Serializable[] attributeKeys) {
        if (key.equals(attributeKeys[ATTRIBUTE_KEY_IDX])) {
            return;
        }

        final String message = "Expected attribute key: '"
                + ATTR_KEY_HEALTH_PROCESSOR + "." + key + ".[true|false]'"
                + "but was: '" + toString(attributeKeys) + "'";
        throw new IllegalArgumentException(message);
    }
}
