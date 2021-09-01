package eu.xenit.alfresco.healthprocessor.reporter;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.reporter.api.ToggleableHealthReporter;
import java.time.Duration;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;

@Slf4j
public class ProgressLoggingHealthReporter extends ToggleableHealthReporter {

    private float lastProgressPercentage;

    @Override
    public void onStart() {
        lastProgressPercentage = 0;
    }

    @Override
    public void onProgress(@Nonnull IndexingProgress progress) {
        if (!log.isInfoEnabled()) {
            return;
        }

        if(progress.isUnknown()) {
            return;
        }

        float progressPercentage = progress.getProgress();
        if (Float.isNaN(progressPercentage)) {
            return;
        }

        // Progress is less than 1%, don't log
        if (progressPercentage - lastProgressPercentage < 0.01) {
            return;
        }

        log.info("Health-Processor iteration {}% completed. ETA: {}",
                Math.round(progressPercentage * 100),
                progress.getEstimatedCompletion().map(duration -> duration.withNanos(0)).map(Duration::toMillis)
                        .map(DurationFormatUtils::formatDurationHMS).orElse("Unknown")
        );
        lastProgressPercentage = progressPercentage;
    }
}
