package eu.xenit.alfresco.healthprocessor.metrics;

public interface MetricFactory {

    TimerMetric createTimer(String name, String... tags);
}
