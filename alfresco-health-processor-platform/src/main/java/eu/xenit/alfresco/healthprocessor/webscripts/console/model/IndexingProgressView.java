package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.indexing.IndexingProgress;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.time.DurationFormatUtils;

@AllArgsConstructor
public class IndexingProgressView {

    private final IndexingProgress indexingProgress;

    public String getProgress() {
        float progress = indexingProgress.getProgress();
        if (Float.isNaN(progress)) {
            return "Unknown";
        }
        return String.format("%.2f%%", progress * 100);
    }

    public Date getStartTime() {
        return Date.from(Instant.now().minus(indexingProgress.getElapsed()));
    }

    public String getElapsed() {
        return DurationFormatUtils.formatDurationHMS(indexingProgress.getElapsed().withNanos(0).toMillis());
    }

    public String getEstimatedCompletion() {
        return indexingProgress.getEstimatedCompletion()
                .map(duration -> duration.withNanos(0))
                .map(Duration::toMillis)
                .map(DurationFormatUtils::formatDurationHMS)
                .orElse("Unknown");
    }

    public Date getEstimatedCompletionTime() {
        return indexingProgress.getEstimatedCompletion()
                .map(duration -> Instant.now().plus(duration))
                .map(Date::from)
                .orElse(null);
    }
}
