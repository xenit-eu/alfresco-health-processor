package eu.xenit.alfresco.healthprocessor.reporter.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Progress indicator for the health processor cycle
 *
 * Gives an indication of the completion status of a health processor cycle.
 */
public interface CycleProgress {

    /**
     * A progress indication can be unknown for a couple of reasons:
     *   * The chosen indexing strategy can not determine its progress in any way
     *   * There is no cycle currently active
     * @return <code>true</code> if the progress is unknown for any reason
     */
    boolean isUnknown();

    /**
     * The cycle completion progress as a percentage
     *
     * @return A number in the range [0;1]. Or <code>NaN</code> if it can not be determined.
     */
    float getProgress();

    /**
     * Total wall-clock time that has elapsed since the health processor cycle has started.
     *
     * @return Non-negative duration. Can be {@link Duration#ZERO} if no cycle is active.
     */
    @Nonnull
    Duration getElapsed();

    /**
     * Estimated wall-clock time until the completion of the current health processor cycle.
     *
     * The result can be an empty {@link Optional} when:
     *  * The chosen indexing strategy can not determine its progress in any way
     *  * There is no cycle currently active
     *  * Not enough progress has been made to create an estimate
     *
     * @return Non-negative duration until the completion of the current cycle. Empty if completion duration can not be determined.
     */
    @Nonnull
    Optional<Duration> getEstimatedCompletion();
}
