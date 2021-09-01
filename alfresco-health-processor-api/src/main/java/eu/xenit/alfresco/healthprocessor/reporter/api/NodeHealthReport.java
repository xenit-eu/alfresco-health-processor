package eu.xenit.alfresco.healthprocessor.reporter.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeHealthReport implements Serializable {

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef) {
        this(status, nodeRef, Collections.emptySet());
    }

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, String... messages) {
        this(status, nodeRef, Arrays.asList(messages));
    }

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, Collection<String> messages) {
        this(status, nodeRef, new HashSet<>(messages));
    }

    NodeHealthStatus status;
    NodeRef nodeRef;
    HashSet<String> messages;

}
