package eu.xenit.alfresco.healthprocessor.plugins.solr.filter;

import eu.xenit.alfresco.healthprocessor.util.QNameUtil;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

@Slf4j
@AllArgsConstructor
public class PropertySolrNodeFilter implements SolrNodeFilter {

    private final NodeService nodeService;
    private final Map<QName, Serializable> filteredProperties;

    public PropertySolrNodeFilter(ServiceRegistry serviceRegistry,
            Map<String, String> filteredProperties) {
        this(
                serviceRegistry.getNodeService(),
                filteredProperties.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> QNameUtil.toQName(e.getKey(), serviceRegistry.getNamespaceService()),
                                Entry::getValue))
        );
    }

    @Override
    public boolean isIgnored(Status nodeRefStatus) {
        if (nodeRefStatus.isDeleted()) {
            log.trace("Skipping deleted node {}", nodeRefStatus.getNodeRef());
            return false;
        }
        try {
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRefStatus.getNodeRef());

            for (Entry<QName, Serializable> filteredProperty : filteredProperties.entrySet()) {
                if (properties.containsKey(filteredProperty.getKey())) {
                    Serializable value = properties.get(filteredProperty.getKey());
                    if (value instanceof List) {
                        List list = (List) value;
                        for (Object val : list) {
                            if (compareValue((Serializable) val, filteredProperty.getValue())) {
                                log.debug("Node {} ignored because property {} is {}", nodeRefStatus.getNodeRef(),
                                        filteredProperty.getKey(), val);
                                return true;
                            }
                        }
                    } else {
                        if (compareValue(value, filteredProperty.getValue())) {
                            log.debug("Node {} ignored because property {} is {}", nodeRefStatus.getNodeRef(),
                                    filteredProperty.getKey(), value);
                            return true;
                        }
                    }
                }
            }
        } catch (InvalidNodeRefException invalidNodeRefException) {
            log.debug("Got an exception while fetching properties for node {}.", nodeRefStatus.getNodeRef(),
                    invalidNodeRefException);
        }

        return false;
    }

    private static boolean compareValue(Serializable a, Serializable b) {
        if(Objects.equals(a, b)) {
            return true;
        } else if(b instanceof String && !(a instanceof String)) {
            return Objects.toString(a).equals(b);
        }
        return false;
    }
}
