package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.time.DurationFormatUtils;

@AllArgsConstructor
public class CycleProgressView {

    private final CycleProgress cycleProgress;

    public boolean isNone() {
        return cycleProgress.isUnknown();
    }

    public String getProgress() {
        if (cycleProgress.isUnknown()) {
            return "Unknown";
        }
        float progress = cycleProgress.getProgress();
        return String.format("%.2f%%", progress * 100);
    }

    public Date getStartTime() {
        return Date.from(Instant.now().minus(cycleProgress.getElapsed()));
    }

    public String getElapsed() {
        return format(cycleProgress.getElapsed().withNanos(0));
    }

    public String getEstimatedCompletion() {
        return cycleProgress.getEstimatedCompletion()
                .map(duration -> duration.withNanos(0))
                .map(CycleProgressView::format)
                .orElse("Unknown");
    }

    public Date getEstimatedCompletionTime() {
        return cycleProgress.getEstimatedCompletion()
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
