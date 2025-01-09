package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import eu.xenit.alfresco.healthprocessor.indexing.NullCycleProgress;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import eu.xenit.alfresco.healthprocessor.indexing.SimpleCycleProgress;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class CycleProgressViewTest {

    @Test
    void isNone_none() {
        CycleProgressView indexingProgressView = new CycleProgressView(NullCycleProgress.getInstance());

        assertThat(indexingProgressView.isNone(), is(equalTo(true)));
    }

    @Test
    void isNone_other() {
        CycleProgressView indexingProgressView = new CycleProgressView(new SimpleCycleProgress(0, 0, () -> 0));

        assertThat(indexingProgressView.isNone(), is(equalTo(false)));
    }

    @Test
    void getProgress_unknown() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
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
        };

        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getProgress(), is(equalTo("Unknown")));
    }

    @Test
    void getProgress_percentage() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
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
        };

        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getProgress().replaceAll(",", "."), is(equalTo("10.00%")));
    }

    @Test
    void getStartTime() {
        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
            }

            @Nonnull
            @Override
            public Duration getElapsed() {
                return Duration.between(now, Instant.now());
            }

            @Nonnull
            @Override
            public Optional<Duration> getEstimatedCompletion() {
                return Optional.empty();
            }
        };

        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getStartTime(), is(equalTo(Date.from(now))));
    }

    @Test
    void getElapsed_short() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
            }

            @Nonnull
            @Override
            public Duration getElapsed() {
                return Duration.of(2, ChronoUnit.HOURS)
                        .plus(5, ChronoUnit.MINUTES)
                        .plus(24, ChronoUnit.SECONDS);
            }

            @Nonnull
            @Override
            public Optional<Duration> getEstimatedCompletion() {
                return Optional.empty();
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getElapsed(), is(equalTo("02:05:24")));
    }

    @Test
    void getElapsed_day() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
            }

            @Nonnull
            @Override
            public Duration getElapsed() {
                return Duration.of(1, ChronoUnit.DAYS);
            }

            @Nonnull
            @Override
            public Optional<Duration> getEstimatedCompletion() {
                return Optional.empty();
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getElapsed(), is(equalTo("1 day 00:00:00")));
    }

    @Test
    void getElapsed_long() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
            }

            @Nonnull
            @Override
            public Duration getElapsed() {
                return Duration.of(2, ChronoUnit.DAYS)
                        .plus(3, ChronoUnit.HOURS)
                        .plus(5, ChronoUnit.MINUTES)
                        .plus(24, ChronoUnit.SECONDS);
            }

            @Nonnull
            @Override
            public Optional<Duration> getEstimatedCompletion() {
                return Optional.empty();
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getElapsed(), is(equalTo("2 days 03:05:24")));
    }

    @Test
    void getElapsed_year() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public float getProgress() {
                return 0.1f;
            }

            @Nonnull
            @Override
            public Duration getElapsed() {
                return Duration.of(365, ChronoUnit.DAYS)
                        .plus(3, ChronoUnit.HOURS)
                        .plus(5, ChronoUnit.MINUTES)
                        .plus(24, ChronoUnit.SECONDS);
            }

            @Nonnull
            @Override
            public Optional<Duration> getEstimatedCompletion() {
                return Optional.empty();
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getElapsed(), is(equalTo("365 days 03:05:24")));
    }

    @Test
    void getEstimatedCompletion_unknown() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
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
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getEstimatedCompletion(), is(equalTo("Unknown")));
    }

    @Test
    void getEstimatedCompletion_time() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
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
                return Optional.of(Duration.of(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getEstimatedCompletion(), is(equalTo("1 day 01:00:00")));
    }

    @Test
    void getEstimatedCompletionTime_normal() {
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
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
                return Optional.of(Duration.between(Instant.now(), tomorrow));
            }
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getEstimatedCompletionTime().toString(), is(equalTo(Date.from(tomorrow).toString())));
    }

    @Test
    void getEstimatedCompletionTime_unknown() {
        CycleProgress mockIndexingProgress = new CycleProgress() {
            @Override
            public boolean isUnknown() {
                return false;
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
        };
        CycleProgressView indexingProgressView = new CycleProgressView(mockIndexingProgress);

        assertThat(indexingProgressView.getEstimatedCompletionTime(), is(nullValue()));
    }
}
