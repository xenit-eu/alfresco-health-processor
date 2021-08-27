package eu.xenit.alfresco.healthprocessor.fixer.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum NodeFixStatus {
    SUCCEEDED(true, true),
    FAILED(false, true),
    SKIPPED(false, false);

    boolean fixed;
    boolean interesting;

}
