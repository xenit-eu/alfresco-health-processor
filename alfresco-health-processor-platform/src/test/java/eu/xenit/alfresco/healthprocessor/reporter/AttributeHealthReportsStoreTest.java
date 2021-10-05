package eu.xenit.alfresco.healthprocessor.reporter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.InMemoryAttributeStore;
import eu.xenit.alfresco.healthprocessor.util.TestReports;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeHealthReportsStoreTest {

    InMemoryAttributeStore attributeStore;
    HealthReportsStore reportsStore;

    @BeforeEach
    void setup() {
        attributeStore = new InMemoryAttributeStore();
        reportsStore = createReportsStore();
    }

    private HealthReportsStore createReportsStore() {
        return new AttributeHealthReportsStore(attributeStore, new NodeHealthReportClassifier(), 20);
    }

    @Test
    void processReports_and_clear() {
        reportsStore.onStart();

        Set<NodeHealthReport> reports = new HashSet<>();
        IntStream.range(0, 10).forEach(i -> reports.add(TestReports.healthy()));
        IntStream.range(0, 5).forEach(i -> reports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, reports);

        Map<Class<? extends HealthProcessorPlugin>, List<NodeHealthReport>> retrievedReports =
                reportsStore.retrieveStoredReports();
        assertThat(retrievedReports, hasEntry(
                is(equalTo(AssertHealthProcessorPlugin.class)),
                hasSize(5)
        ));

        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrievedReportStats =
                reportsStore.retrieveRecordedStats();
        assertThat(retrievedReportStats,
                hasEntry(
                        is(equalTo(AssertHealthProcessorPlugin.class)),
                        hasEntry(equalTo(NodeHealthStatus.HEALTHY), equalTo(10L))
                ));
        assertThat(retrievedReportStats,
                hasEntry(
                        is(equalTo(AssertHealthProcessorPlugin.class)),
                        hasEntry(equalTo(NodeHealthStatus.UNHEALTHY), equalTo(5L))
                ));

        reportsStore.onCycleDone(Collections.emptyList());
        assertThat(reportsStore.retrieveStoredReports(), is(notNullValue()));
        assertThat(reportsStore.retrieveStoredReports().keySet(), is(empty()));
        assertThat(reportsStore.retrieveRecordedStats(), is(notNullValue()));
        assertThat(reportsStore.retrieveRecordedStats().keySet(), is(empty()));

    }

    @Test
    void retrieveReportsAfterRestart() {
        reportsStore.onStart();
        /*
        Issue observed when storing the 'report-stats' attribute with a value of type EnumMap. When retrieving the
        attribute from the DB (e.g. after an Alfresco restart while the cycle is still running), Alfresco returns
        the attribute value as a HashMap instead of an EnumMap.
         */
        Map<NodeHealthStatus, Long> reportStats = new HashMap<>();
        reportStats.put(NodeHealthStatus.HEALTHY, 10L);
        reportStats.put(NodeHealthStatus.UNHEALTHY, 5L);

        attributeStore.setAttribute((Serializable) reportStats, AttributeHealthReportsStore.ATTR_KEY_REPORT_STATS,
                AssertHealthProcessorPlugin.class);

        Set<NodeHealthReport> newReports = new HashSet<>();
        newReports.add(TestReports.healthy());

        reportsStore.processReports(AssertHealthProcessorPlugin.class, newReports);

        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> retrievedReportStats =
                reportsStore.retrieveRecordedStats();
        assertThat(retrievedReportStats,
                hasEntry(
                        is(equalTo(AssertHealthProcessorPlugin.class)),
                        hasEntry(equalTo(NodeHealthStatus.HEALTHY), equalTo(11L))
                ));
        assertThat(retrievedReportStats,
                hasEntry(
                        is(equalTo(AssertHealthProcessorPlugin.class)),
                        hasEntry(equalTo(NodeHealthStatus.UNHEALTHY), equalTo(5L))
                ));

    }

    @Test
    void limitStoredReports() {
        reportsStore.onStart();

        Set<NodeHealthReport> reports = new HashSet<>();
        IntStream.range(0, 40).forEach(i -> reports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, reports);

        Map<Class<? extends HealthProcessorPlugin>, Map<NodeHealthStatus, Long>> reportStats = reportsStore.retrieveRecordedStats();

        assertThat(reportStats,
                hasEntry(
                        is(equalTo(AssertHealthProcessorPlugin.class)),
                        hasEntry(equalTo(NodeHealthStatus.UNHEALTHY), equalTo(40L))
                ));

        List<NodeHealthReport> storedReports = reportsStore.retrieveStoredReports().get(AssertHealthProcessorPlugin.class);

        assertThat(storedReports, hasSize(20));
    }

    @Test
    void limitStoredReports_crash_during_processing() {
        reportsStore.onStart();

        Set<NodeHealthReport> reports = new HashSet<>();
        IntStream.range(0, 15).forEach(i -> reports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, reports);

        // Imagine that Alfresco crashes here, we still don't want to store more data in the Attribute store than configured
        // We create a new reports store instance here to simulate restart
        reportsStore = createReportsStore();

        reportsStore.onStart();

        Set<NodeHealthReport> additionalReports = new HashSet<>();
        IntStream.range(0, 15).forEach(i -> additionalReports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, additionalReports);

        List<NodeHealthReport> storedReports = reportsStore.retrieveStoredReports().get(AssertHealthProcessorPlugin.class);
        assertThat(storedReports, hasSize(20));

        reportsStore.onCycleDone(Collections.emptyList());
    }

    @Test
    void limitStoredReports_crash_after_processing() {
        reportsStore.onStart();

        Set<NodeHealthReport> reports = new HashSet<>();
        IntStream.range(0, 15).forEach(i -> reports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, reports);

        reportsStore.onCycleDone(Collections.emptyList());

        // Imagine that Alfresco crashes here, we want to be able to store a new set of reports here without being limited
        // We create a new reports store instance here to simulate restart
        reportsStore = createReportsStore();

        reportsStore.onStart();

        Set<NodeHealthReport> additionalReports = new HashSet<>();
        IntStream.range(0, 15).forEach(i -> additionalReports.add(TestReports.unhealthy()));

        reportsStore.processReports(AssertHealthProcessorPlugin.class, additionalReports);

        List<NodeHealthReport> storedReports = reportsStore.retrieveStoredReports().get(AssertHealthProcessorPlugin.class);
        assertThat(storedReports, hasSize(15));

        reportsStore.onCycleDone(Collections.emptyList());
    }

}
