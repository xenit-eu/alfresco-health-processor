package eu.xenit.alfresco.healthprocessor.processing;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProcessorConfiguration {

    private final boolean singleTenant;
    private final int nodeBatchSize;
    private final double maxBatchesPerSecond;
    private final boolean readOnly;
    private final String runAsUser;
}
