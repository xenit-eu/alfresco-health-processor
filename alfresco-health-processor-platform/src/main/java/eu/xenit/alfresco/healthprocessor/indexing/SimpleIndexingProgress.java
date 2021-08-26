package eu.xenit.alfresco.healthprocessor.indexing;

import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleIndexingProgress implements IndexingProgress {

    private final long startId;
    private final Instant startTime = Instant.now();
    private final long endId;
    private final LongSupplier currentId;

    private static float interpolate(float start, float end, float current) {
        return clampPercentage((current - start) / (end - start));
    }

    private static float clampPercentage(float in) {
        return Math.max(0, Math.min(1, in));
    }

    @Override
    public float getProgress() {
        return interpolate(startId, endId, currentId.getAsLong());
    }

    @Override
    public Duration getElapsed() {
        return Duration.between(startTime, Instant.now());
    }
}
