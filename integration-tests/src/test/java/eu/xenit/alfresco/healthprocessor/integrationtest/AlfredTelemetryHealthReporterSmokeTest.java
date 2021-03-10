package eu.xenit.alfresco.healthprocessor.integrationtest;

import static io.restassured.RestAssured.given;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AlfredTelemetryHealthReporterSmokeTest extends RestAssuredTest {

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
        given()
                .log().ifValidationFails()
                .when()
                .get(URI_BASE_METRICS + meterName)
                .then()
                .statusCode(200);
    }

}
