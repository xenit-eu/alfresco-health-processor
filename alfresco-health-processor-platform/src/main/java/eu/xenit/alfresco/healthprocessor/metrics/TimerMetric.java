package eu.xenit.alfresco.healthprocessor.metrics;

import java.util.function.Supplier;

public interface TimerMetric {

    void measure(Runnable r);

    <T> T measure(Supplier<T> r);

}
