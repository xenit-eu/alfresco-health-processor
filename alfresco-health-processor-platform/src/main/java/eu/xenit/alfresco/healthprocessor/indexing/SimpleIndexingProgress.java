package eu.xenit.alfresco.healthprocessor.indexing;

import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleIndexingProgress implements IndexingProgress {

    private final long startId;
    private final Instant startTime;
    private final long endId;
    private final LongSupplier currentId;

    public SimpleIndexingProgress(long startId, long endId, LongSupplier currentId) {
        this(startId, Instant.now(), endId, currentId);
    }

    private static float interpolate(long start, long end, long current) {
        // We are casting one side of the division to a float, so it performs float division instead of integer division
        return clampPercentage((float)(current - start) / (end - start));
    }

    private static float clampPercentage(float in) {
        return Math.max(0, Math.min(1, in));
    }

    @Override
    public float getProgress() {
        return interpolate(startId - 1, endId, currentId.getAsLong());
    }

    @Override
    public Duration getElapsed() {
        return Duration.between(startTime, Instant.now());
    }
}
