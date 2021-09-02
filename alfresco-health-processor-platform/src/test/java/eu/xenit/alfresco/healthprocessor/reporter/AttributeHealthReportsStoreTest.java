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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeHealthReportsStoreTest {

    InMemoryAttributeStore attributeStore;
    AttributeHealthReportsStore reportsStore;

    @BeforeEach
    void setup() {
        attributeStore = new InMemoryAttributeStore();
        reportsStore = new AttributeHealthReportsStore(attributeStore);
    }

    @Test
    void processReports_and_clear() {
        HashSet<NodeHealthReport> reports = new HashSet<>();
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

        reportsStore.clear();
        assertThat(reportsStore.retrieveStoredReports(), is(notNullValue()));
        assertThat(reportsStore.retrieveStoredReports().keySet(), is(empty()));
        assertThat(reportsStore.retrieveRecordedStats(), is(notNullValue()));
        assertThat(reportsStore.retrieveRecordedStats().keySet(), is(empty()));

    }

    @Test
    void retrieveReportsAfterRestart() {
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

        HashSet<NodeHealthReport> newReports = new HashSet<>();
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

}
