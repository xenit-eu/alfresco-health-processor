package eu.xenit.alfresco.healthprocessor.webscripts.console;

import lombok.Value;

@Value
public class AdminConsoleWebscriptResponseModel {

    HealthProcessorModule module;

    @Value
    public static class HealthProcessorModule {

        String id;
        String version;
    }
}
