package eu.xenit.alfresco.healthprocessor.integrationtest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HealthProcessorPluginSmokeTest extends RestAssuredTest {

    /**
     * Should be in sync with the cron expression configured in the docker-compose test setup.
     */
    private static final int SCHEDULE_INTERVAL_SECONDS = 20;

    @Test
    void smokeTest_ExampleHealthProcessorPlugin() {
        Long firstNumberOfProcessedNodes = getNumberOfProcessedNodes();

        assertThat(firstNumberOfProcessedNodes, is(notNullValue()));
        assertThat(firstNumberOfProcessedNodes, is(greaterThan(0L)));

        await()
                .atMost(Duration.ofSeconds(SCHEDULE_INTERVAL_SECONDS * 2))
                .pollInterval(Duration.ofSeconds(2))
                .until(this::getNumberOfProcessedNodes, greaterThan(firstNumberOfProcessedNodes));
    }

    private Long getNumberOfProcessedNodes() {
        return getExampleExtensionsInfo().getLong("plugin.processed");
    }
}
