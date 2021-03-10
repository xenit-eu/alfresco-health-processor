package eu.xenit.alfresco.healthprocessor.reporter.telemetry;

public interface Constants {

    interface Key {

        String BASE = "health-processor";

        String ACTIVE = BASE + ".active";
        String PLUGINS = BASE + ".plugins";
        String REPORTS = BASE + ".reports";

    }

    interface Tag {

        String PLUGIN = "plugin";
        String STATUS = "status";

    }

    interface Description {

        String PLUGINS = "Number of registered, active HealthProcessorPlugin implementations";
    }

}
