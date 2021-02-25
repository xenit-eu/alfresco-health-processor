package eu.xenit.alfresco.healthprocessor.reporter.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
@AllArgsConstructor
public class NodeHealthReport {

    public NodeHealthReport(NodeHealthStatus status, NodeRef nodeRef) {
        this(status, nodeRef, Collections.emptySet());
    }

    public NodeHealthReport(NodeHealthStatus status, NodeRef nodeRef, String... messages) {
        this(status, nodeRef, new HashSet<>(Arrays.asList(messages)));
    }

    NodeHealthStatus status;
    NodeRef nodeRef;
    Set<String> messages;

}