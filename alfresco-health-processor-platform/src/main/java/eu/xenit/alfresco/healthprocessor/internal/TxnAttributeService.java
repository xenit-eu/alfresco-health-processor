package eu.xenit.alfresco.healthprocessor.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import jdk.internal.joptsimple.internal.Strings;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.attributes.DuplicateAttributeException;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Might be usable, might not.
 * {@link AttributeService} wrapper that allows different Alfresco nodes to claim ranges of transactions.
 */
public class TxnAttributeService {

    private static final Logger logger = LoggerFactory.getLogger(TxnAttributeService.class);

    private static final String ATTR_KEY_HEALTH_PROCESSOR = "HealthProcessor";
    private static final String ATTR_KEY_TXN_START = "TransactionStart";

    private final AttributeService attributeService;
    private final DescriptorService descriptorService;

    public TxnAttributeService(AttributeService attributeService, DescriptorService descriptorService) {
        this.attributeService = attributeService;
        this.descriptorService = descriptorService;
    }

    public Long getLastTransactionRangeStartId() {
        final Long[] startId = new Long[]{-1L};

        attributeService.getAttributes((Long id, Serializable value, Serializable[] keys) -> {
            logger.debug("Processing attribute id: '{}', keys: '{}', value: '{}'", id, keys, value);
            startId[0] = Long.max(startId[0], extractTransactionStartId(keys));
            return true;
        }, ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_TXN_START);

        return (startId[0] == -1L) ? null : startId[0];
    }

    public boolean acquireTransactionRange(final Long startId) {
        ParameterCheck.mandatory("startId", startId);
        try {
            attributeService.createAttribute(getRepositoryId(), ATTR_KEY_HEALTH_PROCESSOR, ATTR_KEY_TXN_START, startId);
            return true;
        } catch (DuplicateAttributeException e) {
            logger.info("Failed to acquire transaction range with start ID '{}'", startId);
            logger.debug("Cause", e);
            return false;
        }
    }

    public void cleanupAttributes() {
        attributeService.removeAttributes(ATTR_KEY_HEALTH_PROCESSOR);
    }

    private String getRepositoryId() {
        return descriptorService.getCurrentRepositoryDescriptor().getId();
    }

    private static Long extractTransactionStartId(Serializable[] attributeKeys) {
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        if (attributeKeys.length != 3
                || !ATTR_KEY_HEALTH_PROCESSOR.equals(attributeKeys[0])
                || !ATTR_KEY_TXN_START.equals(attributeKeys[1])
                || !(attributeKeys[2] instanceof Long)) {
            final String message = "Expected attribute key combination: '"
                    + ATTR_KEY_HEALTH_PROCESSOR + "." + ATTR_KEY_TXN_START + ".${start-transaction-id}'"
                    + "but was: '" + toString(attributeKeys) + "'";
            throw new IllegalArgumentException(message);
        }
        return (Long) attributeKeys[2];
    }

    private static String toString(Serializable[] attributeKeys) {
        ParameterCheck.mandatory("attributeKeys", attributeKeys);
        return Strings.join(
                Arrays.stream(attributeKeys).map(Object::toString).collect(Collectors.toSet())
                , ",");
    }
}
