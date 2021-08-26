package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Key {

        public static final String BASE = "health-processor";

        public static final String ACTIVE = BASE + ".active";
        public static final String PROGRESS = BASE + ".progress";
        public static final String PLUGINS = BASE + ".plugins";
        public static final String REPORTS = BASE + ".reports";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Tag {

        public static final String PLUGIN = "plugin";
        public static final String STATUS = "status";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Description {

        public static final String PLUGINS = "Number of registered, active HealthProcessorPlugin implementations";
        public static final String PROGRESS = "Completion percentage of the current cycle (0.0-1.0)";
    }

}
