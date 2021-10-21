package eu.xenit.alfresco.healthprocessor.fixer.api;

import eu.xenit.alfresco.healthprocessor.extensibility.annotations.ExtensionType;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.OnlyForUseIn;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport.PersistableData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Attachment to a {@link NodeHealthReport} that contains attempted fixes made on a {@link NodeHealthReport}.
 *
 * Fixes attempted by an {@link HealthFixerPlugin} result in a {@link NodeFixReport} that contains the details about the
 * status of the attempted fix.
 *
 * @since 0.5.0
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class NodeFixReport implements PersistableData {

    /**
     * Result of this fix
     */
    NodeFixStatus fixStatus;

    /**
     * Health report of the attempted fix
     *
     * @implNote This field is transient so it is not serialized, and thus is not available in {@link
     * eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter#onCycleDone(List)}.
     *
     * Since fix reports are already linked via {@link NodeHealthReport#data()}, we can do without this backlink.
     */
    @Getter(onMethod_ = {@OnlyForUseIn(ExtensionType.FIXER)})
    transient NodeHealthReport healthReport;

    /**
     * Messages about this fix
     */
    Set<String> messages;
    NodeRef nodeRef;

    @OnlyForUseIn(ExtensionType.FIXER)
    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport) {
        this(status, healthReport, Collections.emptySet());
    }

    @OnlyForUseIn(ExtensionType.FIXER)
    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport, String... messages) {
        this(status, healthReport, Arrays.asList(messages));
    }

    @OnlyForUseIn(ExtensionType.FIXER)
    public NodeFixReport(@Nonnull NodeFixStatus status, @Nonnull NodeHealthReport healthReport,
            Collection<String> messages) {
        this(status, healthReport, new HashSet<>(messages), healthReport.getNodeRef());
    }
}
