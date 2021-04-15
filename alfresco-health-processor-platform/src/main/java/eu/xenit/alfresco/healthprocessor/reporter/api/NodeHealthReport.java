package eu.xenit.alfresco.healthprocessor.reporter.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
@AllArgsConstructor
public class NodeHealthReport implements Serializable {

    public NodeHealthReport(NodeHealthStatus status, NodeRef nodeRef) {
        this(status, nodeRef, Collections.emptySet());
    }

    public NodeHealthReport(NodeHealthStatus status, NodeRef nodeRef, String... messages) {
        this(status, nodeRef, Arrays.asList(messages));
    }

    public NodeHealthReport(NodeHealthStatus status, NodeRef nodeRef, Collection<String> messages) {
        this(status, nodeRef, new HashSet<>(messages));
    }

    NodeHealthStatus status;
    NodeRef nodeRef;
    Set<String> messages;

}
