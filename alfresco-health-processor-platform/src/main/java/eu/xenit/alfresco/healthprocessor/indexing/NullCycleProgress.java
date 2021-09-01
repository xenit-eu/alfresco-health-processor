package eu.xenit.alfresco.healthprocessor.indexing;

import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;


public final class NullCycleProgress implements CycleProgress {

    private static class NullIndexingProgressHolder {

        private static final CycleProgress INSTANCE = new NullCycleProgress();
    }

    public static CycleProgress getInstance() {
        return NullIndexingProgressHolder.INSTANCE;
    }

    private NullCycleProgress() {

    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    @Override
    public float getProgress() {
        return Float.NaN;
    }

    @Nonnull
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
