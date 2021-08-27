package eu.xenit.alfresco.healthprocessor.reporter.api;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import org.alfresco.service.cmr.repository.NodeRef;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeHealthReport implements Serializable {

    NodeHealthStatus status;
    NodeRef nodeRef;
    HashSet<String> messages;
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    Map<Class<?>, Set<?>> data = new HashMap<>();

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef) {
        this(status, nodeRef, Collections.emptySet());
    }

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, String... messages) {
        this(status, nodeRef, Arrays.asList(messages));
    }

    public NodeHealthReport(@Nonnull NodeHealthStatus status, @Nonnull NodeRef nodeRef, Collection<String> messages) {
        this(status, nodeRef, new HashSet<>(messages));
    }

    @SuppressWarnings("All")
    public <T> Set<T> data(Class<T> clazz) {
        return (Set<T>) data.computeIfAbsent(clazz, e -> new HashSet<T>());
    }

    public Map<Class<?>, Set<?>> data() {
        return Collections.unmodifiableMap(data.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> Collections.unmodifiableSet(e.getValue()))));
    }

    public void data(Map<Class<?>, Set<?>> data) {
        for (Entry<Class<?>, Set<?>> entry : data.entrySet()) {
            data((Class<Object>) entry.getKey()).addAll(entry.getValue());
        }
    }

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

    /**
     * Classes that implement this interface and are added to a health report in the {@link #data(Class)} set are stored in the database
     * and can be accessed in {@link HealthReporter#onCycleDone(List)}. Other stored data is dropped after {@link eu.xenit.alfresco.healthprocessor.reporter.HealthReportsStore#recordReportStats(Class, Set)} has created statistics.
     */
    public interface PersistableData extends Serializable {

    }
}
