package eu.xenit.alfresco.healthprocessor.integrationtest;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.path.json.JsonPath;

import java.time.Duration;

import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;

public abstract class BaseFixerIntegrationTest extends RestAssuredTest
{

    private static final Duration INDEXING_WAIT_DEFAULT = Duration.ofSeconds(120);

    private static final Duration INDEXING_POLL_INTERVAL = Duration.ofSeconds(1);

    protected static final Duration HEALTH_PROCESSOR_MAX_WAIT = Duration.ofSeconds(40);

    protected static final Duration HEALTH_PROCESSOR_POLL_INTERVAL = Duration.ofSeconds(1).dividedBy(2);

    protected boolean isSupportedSearchSubsystem(String subsystemName)
    {
        return given().log().ifValidationFails().when().get("s/xenit/healthprocessor/index/supported").then().statusCode(200).extract()
                .jsonPath().getBoolean(subsystemName);
    }

    protected Long getNumberOfIndexedNodes()
    {
        return getIndexedNodes().getLong("size()");
    }

    protected JsonPath getIndexedNodes()
    {
        return given().log().ifValidationFails().when().get("s/xenit/healthprocessor/index/check").then().statusCode(200).extract()
                .jsonPath();
    }

    protected JsonPath purgeNodes(String fixerType)
    {
        return given().log().ifValidationFails().when().get("s/xenit/healthprocessor/" + fixerType + "/purge").then().statusCode(200)
                .extract().jsonPath();
    }

    protected boolean getHealthProcessorActive()
    {
        return getMeterValue("health-processor.active") == 1L;
    }

    protected long getMeterValue(String meter)
    {
        return given().log().ifValidationFails().when().get("s/alfred/telemetry/metrics/" + meter).then().statusCode(200).extract()
                .jsonPath().getLong("measurements[0].value");
    }

    protected long getHealthProcessorReport(NodeHealthStatus healthStatus, String pluginName)
    {
        return getMeterValue("health-processor.reports?tag=status:" + healthStatus.name() + "&tag=plugin:" + pluginName);
    }

    protected void setActiveSearchSubsystem(String subsystemName)
    {
        given().log().ifValidationFails().when().get("s/xenit/healthprocessor/index/configure?subsystem=" + subsystemName).then()
                .statusCode(200);
    }

    protected void setHealthFixerPlugin(String fixerType, boolean enabled)
    {
        given().log().ifValidationFails().when().get("s/xenit/healthprocessor/" + fixerType + "/configure?enabled=" + enabled).then()
                .statusCode(200);
    }

    protected void waitUntilNodesIndexed(String alias, long nodesToAwait)
    {
        waitUntilNodesIndexed(alias, nodesToAwait, INDEXING_POLL_INTERVAL, INDEXING_WAIT_DEFAULT);
    }

    protected void waitUntilNodesIndexed(String alias, long nodesToAwait, Duration pollInterval, Duration maxWait)
    {
        await(alias).atMost(maxWait).pollInterval(pollInterval).until(this::getNumberOfIndexedNodes, equalTo(nodesToAwait));
    }

    protected void waitUntilHealthProcessorIdle(String alias)
    {
        await(alias).atMost(HEALTH_PROCESSOR_MAX_WAIT).pollInterval(HEALTH_PROCESSOR_POLL_INTERVAL).until(this::getHealthProcessorActive,
                equalTo(false));
    }

}
