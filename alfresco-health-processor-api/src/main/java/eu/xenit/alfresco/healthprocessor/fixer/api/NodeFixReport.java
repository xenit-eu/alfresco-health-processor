package eu.xenit.alfresco.healthprocessor.fixer.api;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport.PersistableData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class NodeFixReport implements PersistableData {

    NodeFixStatus fixStatus;
    transient NodeHealthReport healthReport;
    Set<String> messages;
    NodeRef nodeRef;

    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport) {
        this(status, healthReport, Collections.emptySet());
    }

    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport, String... messages) {
        this(status, healthReport, Arrays.asList(messages));
    }

    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport,
            Collection<String> messages) {
        this(status, healthReport, new HashSet<>(messages), healthReport.getNodeRef());
    }
}
