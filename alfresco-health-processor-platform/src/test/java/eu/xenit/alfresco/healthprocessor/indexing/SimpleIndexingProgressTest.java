package eu.xenit.alfresco.healthprocessor.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.alfresco.healthprocessor.indexing.api.IndexingProgress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SimpleIndexingProgressTest {

    @Test
    void progress_none() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.DAYS);
        IndexingProgress progress = new SimpleIndexingProgress(1, startTime, 10, () -> 0);

        assertEquals(0, progress.getProgress());
        assertThat(progress.getElapsed().withNanos(0), is(Duration.ofDays(1)));
        assertEquals(Optional.empty(), progress.getEstimatedCompletion());
    }

    @Test
    void progress_partial() {
        Instant startTime = Instant.now().minus(9, ChronoUnit.DAYS);
        IndexingProgress progress = new SimpleIndexingProgress(1, startTime, 10, () -> 9);

        assertThat((double) progress.getProgress(), is(closeTo(0.9, 0.01)));
        assertThat(progress.getElapsed().withNanos(0), is(Duration.ofDays(9)));
        assertThat(progress.getEstimatedCompletion().map(d -> d.withNanos(0)), is(Optional.of(Duration.ofDays(1))));
    }

    @Test
    void progress_partial_small() {
        Instant startTime = Instant.now().minus(9, ChronoUnit.DAYS);
        IndexingProgress progress = new SimpleIndexingProgress(1, startTime, 2, () -> 1);

        assertThat((double) progress.getProgress(), is(closeTo(0.5, 0.01)));
        assertThat(progress.getElapsed().withNanos(0), is(Duration.ofDays(9)));
        assertThat(progress.getEstimatedCompletion().map(d -> d.withNanos(0)), is(Optional.of(Duration.ofDays(9))));
    }

    @Test
    void progress_partial_large() {
        Instant startTime = Instant.now().minus(9, ChronoUnit.DAYS);
        IndexingProgress progress = new SimpleIndexingProgress(1, startTime, Long.MAX_VALUE, () -> Long.MAX_VALUE / 2);

        assertThat((double) progress.getProgress(), is(closeTo(0.5, 0.01)));
        assertThat(progress.getElapsed().withNanos(0), is(Duration.ofDays(9)));
        assertThat(progress.getEstimatedCompletion().map(d -> d.withNanos(0)), is(Optional.of(Duration.ofDays(9))));
    }

    @Test
    void progress_complete() {
        Instant startTime = Instant.now().minus(9, ChronoUnit.DAYS);
        IndexingProgress progress = new SimpleIndexingProgress(1, startTime, 10, () -> 10);

        assertThat(progress.getProgress(), is(1.0F));
        assertThat(progress.getElapsed().withNanos(0), is(Duration.ofDays(9)));
        assertThat(progress.getEstimatedCompletion(), is(Optional.of(Duration.ZERO)));
    }

}
