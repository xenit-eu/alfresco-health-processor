package eu.xenit.alfresco.healthprocessor.indexing.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface IndexingProgress {

    float getProgress();

    Duration getElapsed();

    @Nonnull
    default Optional<Duration> getEstimatedCompletion() {
        if (isUnknown()) {
            return Optional.empty();
        }
        long done = (long) (getProgress() * 10_000L);
        long toDo = 10_000L - done;
        if (done == 0) {
            return Optional.empty();
        }
        return Optional.of(getElapsed().dividedBy(done).multipliedBy(toDo));
    }

    default boolean isUnknown() {
        return Float.isNaN(getProgress());
    }
}
