package eu.xenit.alfresco.healthprocessor.processing;

public class ProcConfigUtil {

    private ProcConfigUtil() {
        // private ctor to hide implicit public one
    }

    public static ProcessorConfiguration defaultConfig() {
        return config(true);
    }

    public static ProcessorConfiguration config(boolean singleTenant) {
        return new ProcessorConfiguration(singleTenant, 1000, true, "System");
    }
}
