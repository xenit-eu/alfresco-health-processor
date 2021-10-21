package eu.xenit.alfresco.healthprocessor.reporter.api;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Progress indicator for the health processor cycle.
 *
 * Gives an indication of the completion status of a health processor cycle.
 *
 * @since 0.5.0
 */
public interface CycleProgress {

    /**
     * Whether the progress is unknown for any reason.
     *
     * A progress indication can be unknown for a couple of reasons:
     * <ul>
     *   <li>The chosen indexing strategy can not determine its progress in any way</li>
     *   <li>There is no cycle currently active</li>
     * </ul>
     *
     * @return <code>true</code> if the progress is unknown for any reason
     */
    boolean isUnknown();

    /**
     * The cycle completion progress as a percentage.
     *
     * @return A number in the range [0;1]. Or <code>NaN</code> if it can not be determined.
     */
    float getProgress();

    /**
     * Total wall-clock time that has elapsed since the health processor cycle has started.
     *
     * @return Non-negative duration since the start of the cycle. Can be {@link Duration#ZERO} if no cycle is active.
     */
    @Nonnull
    Duration getElapsed();

    /**
     * Estimated wall-clock time until the completion of the current health processor cycle.
     *
     * The result can be an empty {@link Optional} when:
     * <ul>
     *     <li>The chosen indexing strategy can not determine its progress in any way</li>
     *     <li>There is no cycle currently active</li>
     *     <li>Not enough progress has been made to create an estimate</li>
     * </ul>
     *
     * @return Non-negative estimated duration until the completion of the current cycle. Empty if completion duration
     * can not be determined.
     */
    @Nonnull
    Optional<Duration> getEstimatedCompletion();
}
