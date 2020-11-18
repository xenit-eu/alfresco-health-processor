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
import java.util.stream.Collectors;

@AllArgsConstructor
/**
 * {@link AttributeService} wrapper that allows different Alfresco nodes to claim ranges of transactions.
 */
public class ProcessorAttributeService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorAttributeService.class);

    public static final String ATTR_KEY_HEALTH_PROCESSOR = "HealthProcessor";
    public static final String ATTR_KEY_IS_RUNNING = "IsRunning";

    protected final AttributeService attributeService;
    protected final DescriptorService descriptorService;

    public <T extends Serializable> T getAttribute(final String key, final T defaultValue) {
        final List<T> returnValues = new ArrayList<>();

        if(!attributeService.exists(ATTR_KEY_HEALTH_PROCESSOR,key)) {
            return defaultValue;
        }

        attributeService.getAttributes((Long id, Serializable value, Serializable[] keys) -> {
            logger.debug("Processing attribute id: '{}', keys: '{}', value: '{}'", id, keys, value);
            returnValues.add(extractTransactionStartId(key, keys));
            return true;
        }, ATTR_KEY_HEALTH_PROCESSOR, key);

        return returnValues.stream().findFirst().get();
    }

    public <T extends Serializable> void persistAttribute(final String key, final T value) {
        ParameterCheck.mandatory("value", value);
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

    static <T extends Serializable> T extractTransactionStartId(final String key, Serializable[] attributeKeys){
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        if (attributeKeys.length != 3
            || !ATTR_KEY_HEALTH_PROCESSOR.equals(attributeKeys[0])
            || !key.equals(attributeKeys[1])) {
            final String message = "Expected attribute key combination: '"
                    + ATTR_KEY_HEALTH_PROCESSOR + "." + key + ".[true|false]'"
                    + "but was: '" + toString(attributeKeys) + "'";
            throw new IllegalArgumentException(message);
        }
        return (T) attributeKeys[2];
    }

    static String toString(Serializable[] attributeKeys) {
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        return String.join(",",
                Arrays.stream(attributeKeys)
                            .map(Object::toString)
                            .collect(Collectors.toSet()));
    }
}
