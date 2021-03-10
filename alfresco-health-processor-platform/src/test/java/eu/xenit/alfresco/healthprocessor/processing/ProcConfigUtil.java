package eu.xenit.alfresco.healthprocessor.processing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcConfigUtil {

    public static ProcessorConfiguration defaultConfig() {
        return config(true);
    }

    public static ProcessorConfiguration config(boolean singleTenant) {
        return new ProcessorConfiguration(singleTenant, 1000, -1, true, "System");
    }
}
