package eu.xenit.alfresco.healthprocessor.integrationtest.solr;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;

import org.junit.Assume;
import org.junit.jupiter.api.Test;

import eu.xenit.alfresco.healthprocessor.integrationtest.BaseFixerIntegrationTest;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;

/**
 * This is an end-to-end test of the SolrIndexValidationHealthProcessorPlugin and the SolrIndexNodeFixerPlugin.
 * <p>
 * We expect the validation plugin to *detect* nodes that we purged from the index,
 * followed by the fixer plugin to *index* those nodes again to restore the index.
 */
class SolrIndexFixerIntegrationTest extends BaseFixerIntegrationTest {

    private static final long EXPECTED_INDEXED_NODES = 11L;

    private static final Duration SOLR_INDEXING_WAIT_DEFAULT = Duration.ofSeconds(120);
    private static final Duration SOLR_INDEXING_MAX_WAIT = SOLR_INDEXING_WAIT_DEFAULT.multipliedBy(4);
    private static final Duration SOLR_INDEXING_POLL_INTERVAL = Duration.ofSeconds(1);

    private static final String SOLR_PLUGIN_NAME = "SolrIndexValidationHealthProcessorPlugin";

    @Test
    void reindexesNodesAfterPurge() {
        Assume.assumeTrue("solr6 is not supported", isSupportedSearchSubsystem("solr6"));

        // Disable health fixer plugin
        setHealthFixerPlugin("solr", false);

        // make sure solr index subsystem is enabled
        setActiveSearchSubsystem("solr6");

        // Wait until nodes are all indexed in solr
        waitUntilNodesIndexed("Wait for initial index to be completed", EXPECTED_INDEXED_NODES, SOLR_INDEXING_POLL_INTERVAL,
                SOLR_INDEXING_MAX_WAIT);

        // Wait for the health processor to become IDLE and it has had at least one iteration
        await("Until some health reports have been received")
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.HEALTHY, SOLR_PLUGIN_NAME), greaterThan(0L));
        waitUntilHealthProcessorIdle("Wait for health processor being finished to record number of healthy nodes");
        long allHealthyNodes = getHealthProcessorReport(NodeHealthStatus.HEALTHY, SOLR_PLUGIN_NAME);

        // Purge nodes from solr index
        purgeNodes("solr");
        // Wait until there are no more indexed nodes (they have been purged from the index by solr in the next maintenance interval)
        waitUntilNodesIndexed("Wait until nodes have been purged from the index", 0L);

        // Wait until our health checker has detected less healthy nodes than before
        await("Until non-healthy nodes are detected")
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.HEALTHY, SOLR_PLUGIN_NAME), lessThan(allHealthyNodes));

        // Then wait until health processor is IDLE
        waitUntilHealthProcessorIdle("Until health processor cycle has completed to detect number of fixed nodes");

        // Check that unhealthy reports are received for unindexed nodes
        assertThat(getHealthProcessorReport(NodeHealthStatus.UNHEALTHY, SOLR_PLUGIN_NAME), equalTo(EXPECTED_INDEXED_NODES));

        // Enable health fixer plugin again
        setHealthFixerPlugin("solr", true);

        // Wait until our health checker has re-run and has fixed the issues
        await("Until fixed nodes are detected")
                .atMost(HEALTH_PROCESSOR_MAX_WAIT)
                .pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.FIXED, SOLR_PLUGIN_NAME), equalTo(EXPECTED_INDEXED_NODES));

        // And then that the purged nodes are indexed again
        waitUntilNodesIndexed("Wait until fixed nodes have been reindexed", EXPECTED_INDEXED_NODES);
    }
}
