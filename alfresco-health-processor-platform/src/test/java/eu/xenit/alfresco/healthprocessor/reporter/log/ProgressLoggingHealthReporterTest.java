package eu.xenit.alfresco.healthprocessor.reporter.log;

import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import org.junit.jupiter.api.Test;

class ProgressLoggingHealthReporterTest {

    @Test
    void smokeTest() {
        ProgressLoggingHealthReporter reporter = new ProgressLoggingHealthReporter();

        reporter.onStart();
        reporter.onProgress(NullCycleProgress.getInstance());
    }

}
