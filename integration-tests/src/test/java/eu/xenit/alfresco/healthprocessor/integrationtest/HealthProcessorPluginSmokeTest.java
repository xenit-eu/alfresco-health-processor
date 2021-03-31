package eu.xenit.alfresco.healthprocessor.integrationtest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HealthProcessorPluginSmokeTest extends RestAssuredTest {

    @Test
    void smokeTest_ExampleHealthProcessorPlugin() {
        Long firstNumberOfProcessedNodes = getNumberOfProcessedNodes();

        assertThat(firstNumberOfProcessedNodes, is(notNullValue()));
        assertThat(firstNumberOfProcessedNodes, is(greaterThanOrEqualTo(0L)));

        await()
                .atMost(Duration.ofSeconds(MAX_POLL_TIME_SECONDS))
                .pollInterval(Duration.ofSeconds(2))
                .until(this::getNumberOfProcessedNodes, greaterThan(firstNumberOfProcessedNodes));
    }

    private Long getNumberOfProcessedNodes() {
        return getExampleExtensionsInfo().getLong("plugin.processed");
    }
}
