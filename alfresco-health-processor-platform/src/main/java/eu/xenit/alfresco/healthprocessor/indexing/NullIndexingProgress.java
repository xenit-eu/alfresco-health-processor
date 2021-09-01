package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;


public final class NullIndexingProgress implements IndexingProgress {

    private static class NullIndexingProgressHolder {

        private static final IndexingProgress INSTANCE = new NullIndexingProgress();
    }

    public static IndexingProgress getInstance() {
        return NullIndexingProgressHolder.INSTANCE;
    }

    private NullIndexingProgress() {

    }

    @Override
    public boolean isUnknown() {
        return true;
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
