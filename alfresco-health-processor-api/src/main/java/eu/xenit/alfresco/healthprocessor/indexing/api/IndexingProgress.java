package eu.xenit.alfresco.healthprocessor.indexing.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface IndexingProgress {

    boolean isUnknown();

    float getProgress();

    Duration getElapsed();

    @Nonnull
    Optional<Duration> getEstimatedCompletion();
}
