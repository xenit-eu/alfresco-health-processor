package eu.xenit.alfresco.healthprocessor.indexing.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Singular;


final class NullIndexingProgress implements IndexingProgress {

    private static class NullIndexingProgressHolder {

        private static final IndexingProgress INSTANCE = new NullIndexingProgress();
    }

    public static IndexingProgress getInstance() {
        return NullIndexingProgressHolder.INSTANCE;
    }

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
