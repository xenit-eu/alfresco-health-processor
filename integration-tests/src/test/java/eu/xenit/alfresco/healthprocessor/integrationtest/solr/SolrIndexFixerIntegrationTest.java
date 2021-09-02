package eu.xenit.alfresco.healthprocessor.integrationtest.solr;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import eu.xenit.alfresco.healthprocessor.integrationtest.RestAssuredTest;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * This is an end-to-end test of the SolrIndexValidationHealthProcessorPlugin and the SolrIndexNodeFixerPlugin.
 * <p>
 * We expect the validation plugin to *detect* nodes that we purged from the index,
 * followed by the fixer plugin to *index* those nodes again to restore the index.
 */
class SolrIndexFixerIntegrationTest extends RestAssuredTest {

    private static final long EXPECTED_INDEXED_NODES = 11L;

    private static final Duration SOLR_INDEXING_WAIT_DEFAULT = Duration.ofSeconds(60);
    private static final Duration SOLR_INDEXING_MAX_WAIT = SOLR_INDEXING_WAIT_DEFAULT.multipliedBy(4);
    private static final Duration SOLR_INDEXING_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Duration HEALTH_PROCESSOR_MAX_WAIT = Duration.ofSeconds(40);
    private static final Duration HEALTH_PROCESSOR_POLL_INTERVAL = Duration.ofSeconds(1).dividedBy(2);

    private static final String SOLR_PLUGIN_NAME = "SolrIndexValidationHealthProcessorPlugin";

    @Test
    void reindexesNodesAfterPurge() {
        // Wait until nodes are all indexed in solr
        waitUntilNodesIndexed("Wait for initial index to be completed", EXPECTED_INDEXED_NODES, SOLR_INDEXING_MAX_WAIT);

        // Wait for the health processor to become IDLE and it has had at least one iteration
        await("Until some health reports have been received")
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.HEALTHY), greaterThan(0L));
        waitUntilHealthProcessorIdle("Wait for health processor being finished to record number of healthy nodes");
        long allHealthyNodes = getHealthProcessorReport(NodeHealthStatus.HEALTHY);

        // Purge nodes now
        purgeNodes();

        // Wait until there are no more indexed nodes (they have been purged from the index by solr in the next maintenance interval)
        waitUntilNodesIndexed("Wait until nodes have been purged from the index", 0L);

        // Wait until our health checker has detected less healthy nodes than before
        await("Until non-healthy nodes are detected")
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.HEALTHY), lessThan(allHealthyNodes));

        // Then wait until health processor is IDLE
        waitUntilHealthProcessorIdle("Until health processor cycle has completed to detect number of fixed nodes");

        // Check fixed reports is the number of nodes that we purged
        assertThat(getHealthProcessorReport(NodeHealthStatus.FIXED), equalTo(EXPECTED_INDEXED_NODES));

        // And then that the purged nodes are indexed again
        waitUntilNodesIndexed("Wait until fixed nodes have been reindexed", EXPECTED_INDEXED_NODES);
    }


    private Long getNumberOfIndexedNodes() {
        return getIndexedNodes().getLong("size()");
    }

    private JsonPath getIndexedNodes() {
        return given()
                .log().ifValidationFails()
                .when()
                .get("s/xenit/healthprocessor/solr/check")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private JsonPath purgeNodes() {
        return given()
                .log().ifValidationFails()
                .when()
                .get("s/xenit/healthprocessor/solr/purge")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private boolean getHealthProcessorActive() {
        return getMeterValue("health-processor.active") == 1L;
    }

    private long getMeterValue(String meter) {
        return given()
                .log().ifValidationFails()
                .when()
                .get("s/alfred/telemetry/metrics/" + meter)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("measurements[0].value");
    }

    private long getHealthProcessorReport(NodeHealthStatus healthStatus) {
        return getMeterValue(
                "health-processor.reports?tag=status:" + healthStatus.name() + "&tag=plugin:" + SOLR_PLUGIN_NAME);
    }

    private void waitUntilNodesIndexed(String alias, long nodesToAwait) {
        waitUntilNodesIndexed(alias, nodesToAwait, SOLR_INDEXING_WAIT_DEFAULT);
    }

    private void waitUntilNodesIndexed(String alias, long nodesToAwait, Duration maxWait) {
        await(alias)
                .atMost(maxWait)
                .pollInterval(SOLR_INDEXING_POLL_INTERVAL)
                .until(this::getNumberOfIndexedNodes, equalTo(nodesToAwait));
    }

    private void waitUntilHealthProcessorIdle(String alias) {
        await(alias)
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(this::getHealthProcessorActive, equalTo(false));
    }
}
