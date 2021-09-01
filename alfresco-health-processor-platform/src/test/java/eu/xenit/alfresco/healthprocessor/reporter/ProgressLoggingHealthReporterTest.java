package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.indexing.AssertIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingProgress;
import org.junit.jupiter.api.Test;

class ProgressLoggingHealthReporterTest {

    @Test
    void smokeTest() {
        ProgressLoggingHealthReporter reporter = new ProgressLoggingHealthReporter();

        reporter.onStart();
        reporter.onProgress(AssertIndexingStrategy.class, IndexingProgress.NONE);
    }

}
