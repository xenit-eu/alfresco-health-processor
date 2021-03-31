package eu.xenit.alfresco.healthprocessor.integrationtest;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AlfredTelemetryHealthReporterSmokeTest extends RestAssuredTest {

    private static final String URI_BASE_METRICS = "s/alfred/telemetry/metrics/";

    static Stream<String> namesOfMetersThatShouldBeAvailable() {
        return Stream.of(
                "health-processor.active",
                "health-processor.plugins",
                "health-processor.reports"
        );
    }

    @ParameterizedTest
    @MethodSource("namesOfMetersThatShouldBeAvailable")
    void assertMeterAvailable(String meterName) {
        await()
                .atMost(Duration.ofSeconds(MAX_POLL_TIME_SECONDS))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> getMeter(meterName), equalTo(200));
    }

    private int getMeter(String meterName) {
        return given()
                .log().ifValidationFails()
                .when()
                .get(URI_BASE_METRICS + meterName)
                .getStatusCode();
    }

}
