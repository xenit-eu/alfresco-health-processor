package eu.xenit.alfresco.healthprocessor.indexing.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface IndexingProgress {

    IndexingProgress NONE = NullIndexingProgress.getInstance();

    float getProgress();

    Duration getElapsed();

    @Nonnull
    default Optional<Duration> getEstimatedCompletion() {
        long done = (long) (getProgress() * 10_000L);
        long toDo = 10_000L - done;
        if (done == 0) {
            return Optional.empty();
        }
        return Optional.of(getElapsed().dividedBy(done).multipliedBy(toDo));
    }
}
