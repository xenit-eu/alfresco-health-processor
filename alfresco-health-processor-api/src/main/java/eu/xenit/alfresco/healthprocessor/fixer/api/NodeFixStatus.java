package eu.xenit.alfresco.healthprocessor.fixer.api;

/**
 * Status of a {@link NodeFixReport}
 *
 * @since 0.5.0
 */
public enum NodeFixStatus {
    /**
     * Fix attempt has succeeded
     *
     * The fixer has succeeded in fixing an unhealthy node.
     *
     * Note that certain fixes may be asynchronous, in which case this status means that a fix was scheduled
     * successfully.
     */
    SUCCEEDED,
    /**
     * Fix attempt has failed
     *
     * The fixer has attempted to fix an unhealthy node, but it failed to do so successfully.
     */
    FAILED,
    /**
     * Fix was not attempted
     *
     * The fact that no fix was attempted can have various causes:
     * <ul>
     *     <li>The fixer does not know how to fix a particular problem</li>
     *     <li>The fixer determined that it is not possible for it to fix a particular problem</li>
     * </ul>
     */
    SKIPPED
}
