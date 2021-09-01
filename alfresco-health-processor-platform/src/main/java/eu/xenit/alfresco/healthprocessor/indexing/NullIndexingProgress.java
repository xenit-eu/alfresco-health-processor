package eu.xenit.alfresco.healthprocessor.indexing;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;


final class NullIndexingProgress implements IndexingProgress {

    static final NullIndexingProgress INSTANCE = new NullIndexingProgress();

    private NullIndexingProgress() {

    }

    @Override
    public float getProgress() {
        return Float.NaN;
    }

    @Override
    public Duration getElapsed() {
        return Duration.ZERO;
    }

    @Nonnull
    @Override
    public Optional<Duration> getEstimatedCompletion() {
        return Optional.empty();
    }
}
