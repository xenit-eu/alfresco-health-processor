package eu.xenit.alfresco.healthprocessor.reporter.api;

import eu.xenit.alfresco.healthprocessor.extensibility.annotations.ExtensionType;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.InternalUseOnly;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.NotForUseIn;
import eu.xenit.alfresco.healthprocessor.extensibility.annotations.OnlyForUseIn;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import lombok.*;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * A health report for an Alfresco node.
 *
 * A health report minimally consists of the {@link #getNodeRef()} that is assigned a certain healthyness-status {@link
 * #getStatus()}.
 *
 * It can optionally contain additional information about why it is in a certain health status:
 * <ul>
 *     <li>in human-readable format with {@link #getMessages()}.</li>
 *     <li>in computer-readable format with {@link #data(Class)}, which can contain arbitrary objects with additional information.</li>
 * </ul>
 *
 * {@link HealthReporter}s are allowed, but not are required to process information from {@link #data(Class)}
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeHealthReport implements Serializable {

    NodeHealthStatus status;
    NodeRef nodeRef;
    Set<String> messages;

    /**
     * Arbitrary computer-readable data storage.
     *
     * For type-safety, data is stored by its {@link Class} and are always stored in a {@link Set}, allowing 0, 1 or
     * more items of a class to be present and avoiding any null-pointer problems.
     *
     * @implNote Because this field is not immutable, it is excluded from generated {@link #equals(Object)} and {@link
     * #hashCode()} methods. Because it potentially contains a lot of data, it is also excluded from {@link
     * #toString()}.
     * @since 0.5.0
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    Map<Class<?>, Set<?>> data = new HashMap<>();

    @OnlyForUseIn(ExtensionType.PROCESSOR)
    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef) {
        this(status, nodeRef, Collections.emptySet());
    }

    @OnlyForUseIn(ExtensionType.PROCESSOR)
    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, String... messages) {
        this(status, nodeRef, Arrays.asList(messages));
    }

    @OnlyForUseIn(ExtensionType.PROCESSOR)
    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, Collection<String> messages) {
        this(status, nodeRef, Collections.unmodifiableSet(new HashSet<>(messages)));
    }

    /**
     * Retrieve mutable data-{@link Set} of a certain type.
     *
     * If no data of this type is stored on the object yet, a new, empty {@link Set} is created.
     *
     * @param clazz The {@link Class} of the data-type to retrieve
     * @param <T> The data-type to retrieve
     * @return A mutable Set with all instances of T linked to this report.
     * @since 0.5.0
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> data(Class<T> clazz) {
        return (Set<T>) data.computeIfAbsent(clazz, e -> new HashSet<T>());
    }

    /**
     * Retrieve all computer-readable data stored on this report.
     *
     * You probably don't need to use this method, unless you need to create a copy of a health report.
     *
     * @return Immutable map of all computer-readable data stored on this report.
     * @see #data(Class) for easier access to stored data.
     * @since 0.5.0
     */
    public Map<Class<?>, Set<?>> data() {
        return Collections.unmodifiableMap(data.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> Collections.unmodifiableSet(e.getValue()))));
    }

    /**
     * Store additional computer-readable data to this report.
     *
     * You probably don't need to use this method, unless you are creating a copy of a health report.
     *
     * @param data Map of data to add to this report.
     * @see #data(Class) for easier access to stored data.
     * @since 0.5.0
     */
    @NotForUseIn(ExtensionType.REPORTER)
    @SuppressWarnings("unchecked")
    public void data(Map<Class<?>, Set<?>> data) {
        for (Entry<Class<?>, Set<?>> entry : data.entrySet()) {
            data((Class<Object>) entry.getKey()).addAll(entry.getValue());
        }
    }

    /**
     * Creates a copy of this healthreport without all {@link #data(Class)} stored on the health report that is not
     * marked as {@link PersistableData}.
     *
     * This does not modify the health report, but creates a copy instead.
     *
     * @return A copy of this health report with only {@link PersistableData} {@link #data(Class)}.
     * @since 0.5.0
     */
    @InternalUseOnly
    public NodeHealthReport withoutUnpersistableData() {
        NodeHealthReport nodeHealthReport = new NodeHealthReport(status, nodeRef, messages);
        Map<Class<?>, Set<?>> newData = new HashMap<>(data.size());
        for (Entry<Class<?>, Set<?>> classSetEntry : data.entrySet()) {
            if (PersistableData.class.isAssignableFrom(classSetEntry.getKey())) {
                newData.put(classSetEntry.getKey(), classSetEntry.getValue());
            }
        }
        nodeHealthReport.data(newData);
        return nodeHealthReport;
    }

    public static @NonNull Set<NodeHealthReport> of(@NonNull NodeHealthStatus status, @NonNull Collection<NodeRef> nodeRefs, @NonNull String... messages) {
        return nodeRefs.stream()
                .map(nodeRef -> new NodeHealthReport(status, nodeRef, messages))
                .collect(Collectors.toSet());
    }

    public static @NonNull Set<NodeHealthReport> ofHealthy(@NonNull Collection<NodeRef> nodeRefs, @NonNull String... messages) {
        return of(NodeHealthStatus.HEALTHY, nodeRefs, messages);
    }

    public static @NonNull Set<NodeHealthReport> ofUnhealthy(@NonNull Collection<NodeRef> nodeRefs, @NonNull String... messages) {
        return of(NodeHealthStatus.UNHEALTHY, nodeRefs, messages);
    }

    /**
     * Classes that implement this interface and are added to a health report in the {@link #data(Class)} set are stored
     * in the database and can be accessed after a cycle has finished in {@link HealthReporter#onCycleDone(List)}
     *
     * Other stored data is only available in the {@link HealthReporter#processReports(Class, Set)} method during a
     * cycle.
     *
     * @since 0.5.0
     */
    public interface PersistableData extends Serializable {

    }
}
