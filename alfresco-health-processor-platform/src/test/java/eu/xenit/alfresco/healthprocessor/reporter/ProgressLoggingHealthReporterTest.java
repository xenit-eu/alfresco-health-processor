package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import org.junit.jupiter.api.Test;

class ProgressLoggingHealthReporterTest {

    @Test
    void smokeTest() {
        ProgressLoggingHealthReporter reporter = new ProgressLoggingHealthReporter();

        reporter.onStart();
        reporter.onProgress(IndexingProgress.NONE);
    }

}
