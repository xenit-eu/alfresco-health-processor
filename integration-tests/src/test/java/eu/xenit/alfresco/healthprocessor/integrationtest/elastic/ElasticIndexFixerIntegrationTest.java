package eu.xenit.alfresco.healthprocessor.integrationtest.elastic;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.icu.impl.Assert;

import java.io.IOException;
import java.time.Duration;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assume;
import org.junit.jupiter.api.Test;
import org.springframework.extensions.webscripts.Status;

import eu.xenit.alfresco.healthprocessor.executors.JSONResponseHandler;
import eu.xenit.alfresco.healthprocessor.integrationtest.BaseFixerIntegrationTest;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;

/**
 * This is an end-to-end test of the ElasticIndexValidationHealthProcessorPlugin and the ElasticIndexNodeFixerPlugin.
 * <p>
 * We expect the validation plugin to *detect* nodes that we purged from the index,
 * followed by the fixer plugin to *index* those nodes again to restore the index.
 */
class ElasticIndexFixerIntegrationTest extends BaseFixerIntegrationTest
{

    private static final int ELASTIC_INIT_WAIT = 22500;

    private static final long EXPECTED_INDEXED_NODES = 11L;

    private static final Duration ELASTIC_INDEXING_WAIT_DEFAULT = Duration.ofSeconds(30);

    private static final Duration ELASTIC_INDEXING_MAX_WAIT = ELASTIC_INDEXING_WAIT_DEFAULT.multipliedBy(2);

    private static final Duration ELASTIC_INDEXING_POLL_INTERVAL = Duration.ofSeconds(1);

    private static final String ELASTIC_PLUGIN_NAME = "ElasticIndexValidationHealthProcessorPlugin";

    @Test
    void indexNodesAndPurge()
    {
        Assume.assumeTrue("elasticsearch is not supported", isSupportedSearchSubsystem("elasticsearch"));

        // disable fixer plugin
        setHealthFixerPlugin("elastic", false);

        ensureCleanEmptyElasticState();

        // Docker Compose setup does not include a re-indexer app, so initially the index is empty
        // TODO sometimes throws HTTP 500?
        Long numberOfIndexedNodes = getNumberOfIndexedNodes();
        assertThat(numberOfIndexedNodes, equalTo(0l));

        // enable fixer plugin
        setHealthFixerPlugin("elastic", true);

        // Wait for the health processor to become IDLE and it has had at least one iteration
        await("Until some health reports have been received").atMost(ELASTIC_INDEXING_MAX_WAIT).pollInterval(ELASTIC_INDEXING_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.FIXED, ELASTIC_PLUGIN_NAME), greaterThanOrEqualTo(EXPECTED_INDEXED_NODES));
        waitUntilHealthProcessorIdle("Wait for health processor being finished to record number of fixed nodes");

        numberOfIndexedNodes = getNumberOfIndexedNodes();
        assertThat(numberOfIndexedNodes, equalTo(EXPECTED_INDEXED_NODES));

        // disable fixer plugin
        setHealthFixerPlugin("elastic", false);

        // Purge nodes from elastic index
        purgeNodes("elastic");

        // Wait until there are no indexed nodes (only looks at the subset we intentionally purged)
        waitUntilNodesIndexed("Wait until nodes have been purged from the index", 0L);

        numberOfIndexedNodes = getNumberOfIndexedNodes();
        assertThat(numberOfIndexedNodes, equalTo(0l));

        // Wait until our health checker has detected fewer healthy nodes than before
        await("Until non-healthy nodes are detected").atMost(ELASTIC_INDEXING_MAX_WAIT).pollInterval(ELASTIC_INDEXING_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.UNHEALTHY, ELASTIC_PLUGIN_NAME), equalTo(EXPECTED_INDEXED_NODES));

        // Enable health fixer plugin again
        setHealthFixerPlugin("elastic", true);

        // Wait until our health checker has re-run and has fixed the issues
        await("Until fixed nodes are detected").atMost(ELASTIC_INDEXING_MAX_WAIT).pollInterval(ELASTIC_INDEXING_POLL_INTERVAL)
                .until(() -> getHealthProcessorReport(NodeHealthStatus.FIXED, ELASTIC_PLUGIN_NAME), equalTo(EXPECTED_INDEXED_NODES));

        // And then that the purged nodes are indexed again
        waitUntilNodesIndexed("Wait until fixed nodes have been reindexed", EXPECTED_INDEXED_NODES);
    }

    private void ensureCleanEmptyElasticState()
    {
        // (partial) index created during Docker Compose startup via live indexer is useless
        // index MUST always be initialised by ACS before either re-indexer or live indexer add data
        // otherwise there will be a conflict when ACS tries to update the mapping on subsystem activation (i.e. field type conflict on
        // existing 'cm%3Aname' field)
        deleteIndex();

        // make sure elasticsearch index subsystem is enabled
        setActiveSearchSubsystem("elasticsearch");

        // elasticsearch init is an async thread
        // (this means elastic subsystem isn't really usable immediately after startup or activation when index is first being initialised)
        try
        {
            Thread.sleep(ELASTIC_INIT_WAIT);
        }
        catch (InterruptedException e)
        {
            // reset flag
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // first elasticsearch start does create a partial index state too during first enablement
        // this includes entries for nodes related to a persisted scheduled elastic (validation?) action
        // these entries will be partial (without PATH) due to parents not existing for parent path resolution
        // so run the subsystem + delete-index + init shuffle again
        
        setActiveSearchSubsystem("solr6");
        deleteIndex();
        setActiveSearchSubsystem("elasticsearch");
        
        try
        {
            Thread.sleep(ELASTIC_INIT_WAIT);
        }
        catch (InterruptedException e)
        {
            // reset flag
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void deleteIndex()
    {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
        {
            HttpDelete delIndex = new HttpDelete("http://localhost:9200/alfresco");
            try
            {
                JsonNode res = httpClient.execute(delIndex, new JSONResponseHandler());
                if (!res.has("acknowledged") || !res.get("acknowledged").asBoolean())
                {
                    Assert.fail("Failed to delete (partial) ElasticSearch index");
                }
            }
            finally
            {
                delIndex.reset();
            }
        }
        catch (HttpResponseException e)
        {
            if (e.getStatusCode() != Status.STATUS_NOT_FOUND)
            {
                Assert.fail(e);
            }
        }
        catch (IOException e)
        {
            Assert.fail(e);
        }
    }
}
