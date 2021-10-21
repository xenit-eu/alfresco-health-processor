package eu.xenit.alfresco.healthprocessor.fixer.api;

import eu.xenit.alfresco.healthprocessor.extensibility.annotations.ExtensionType;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.InternalUseOnly;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.OnlyForUseIn;
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
import lombok.Getter;
import lombok.ToString;
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
     * @implNote This field is transient, so it is not serialized, to avoid a circular reference.
     *
     * This means this link will not be available after the HealthReportsStore has stored a {@link NodeHealthReport}. It
     * is only used to be able to link a {@link NodeFixReport} returned from {@link HealthFixerPlugin#fix(Class, Set)}
     * to its corresponding {@link NodeHealthReport} in HealthProcessor internals.
     *
     * The HealthProcessor fixers component will link it via {@link NodeHealthReport#data()}, so we have no need for
     * this backlink afterwards.
     */
    @Getter(onMethod_ = {@OnlyForUseIn(ExtensionType.FIXER)})
    transient NodeHealthReport healthReport;

    /**
     * Messages about this fix
     */
    Set<String> messages;

    /**
     * Internal, persistent reference to the node of the {@link #healthReport}.
     *
     * @implNote This field is here so the serialized {@link NodeFixReport} has a reference to the node it is about.
     * This is necessary for when {@link NodeFixReport}s are placed in a set (as in the return value of {@link
     * HealthFixerPlugin#fix(Class, Set)}), because the transient {@link #healthReport} field is not part of {@link
     * #hashCode()} and {@link #equals(Object)} calculations.
     */
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    @InternalUseOnly
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
