package eu.xenit.alfresco.healthprocessor.reporter.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NodeHealthStatus {
    HEALTHY(false),
    UNHEALTHY(true),
    NONE(false),
    UNREPORTED(true),
    FIXED(true);

    @Getter
    private final boolean interesting;
}
