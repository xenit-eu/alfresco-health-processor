package eu.xenit.alfresco.healthprocessor.integrationtest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

public class HealthProcessorPluginSmokeTest extends RestAssuredTest {

    /**
     * Should be in sync with the cron expression configured in the docker-compose test setup.
     */
    private static final int SCHEDULE_INTERVAL_SECONDS = 20;

    @Test
    void smokeTest_ExampleHealthProcessorPlugin() {
        JsonPath firstResponse = getExampleExtensionsInfo();
        Long firstNumberOfProcessedNodes = firstResponse.getLong("plugin.processed");

        assertThat(firstNumberOfProcessedNodes, is(notNullValue()));
        assertThat(firstNumberOfProcessedNodes, is(greaterThan(0L)));

        waitUntilNextIterationShouldBeDone();

        JsonPath secondResponse = getExampleExtensionsInfo();
        Long secondNumberOfProcessedNodes = secondResponse.getLong("plugin.processed");

        assertThat(secondNumberOfProcessedNodes, is(notNullValue()));
        assertThat(secondNumberOfProcessedNodes, is(greaterThan(0L)));
        assertThat(secondNumberOfProcessedNodes, is(greaterThan(firstNumberOfProcessedNodes)));
    }

    private void waitUntilNextIterationShouldBeDone() {
        int secondsToWait = SCHEDULE_INTERVAL_SECONDS + SCHEDULE_INTERVAL_SECONDS / 2;
        try {
            Thread.sleep(1000 * secondsToWait);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
