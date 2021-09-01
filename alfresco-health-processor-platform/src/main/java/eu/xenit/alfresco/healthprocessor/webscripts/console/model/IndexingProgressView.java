package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.time.DurationFormatUtils;

@AllArgsConstructor
public class IndexingProgressView {

    private final IndexingProgress indexingProgress;

    public boolean isNone() {
        return indexingProgress.isUnknown();
    }

    public String getProgress() {
        if (indexingProgress.isUnknown()) {
            return "Unknown";
        }
        float progress = indexingProgress.getProgress();
        return String.format("%.2f%%", progress * 100);
    }

    public Date getStartTime() {
        return Date.from(Instant.now().minus(indexingProgress.getElapsed()));
    }

    public String getElapsed() {
        return format(indexingProgress.getElapsed().withNanos(0));
    }

    public String getEstimatedCompletion() {
        return indexingProgress.getEstimatedCompletion()
                .map(duration -> duration.withNanos(0))
                .map(IndexingProgressView::format)
                .orElse("Unknown");
    }

    public Date getEstimatedCompletionTime() {
        return indexingProgress.getEstimatedCompletion()
                .map(duration -> Instant.now().plus(duration))
                .map(Date::from)
                .orElse(null);
    }

    private static String format(Duration duration) {
        long fullDays = duration.toDays();
        Duration rest = duration.minusDays(fullDays);
        StringBuilder formattedDuration = new StringBuilder();
        if (fullDays > 0) {
            formattedDuration.append(fullDays)
                    .append(' ')
                    .append("day");
            if (fullDays > 1) {
                formattedDuration.append('s');
            }
            formattedDuration.append(' ');
        }

        formattedDuration.append(DurationFormatUtils.formatDuration(rest.toMillis(), "HH:mm:ss"));

        return formattedDuration.toString();
    }
}
