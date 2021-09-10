package eu.xenit.alfresco.healthprocessor.metrics;

import java.util.function.Supplier;

public class NullMetricFactory implements MetricFactory {

    @Override
    public TimerMetric createTimer(String name, String... tags) {
        return new NullTimer();
    }

    private static class NullTimer implements TimerMetric {

        @Override
        public void measure(Runnable r) {
            r.run();
        }

        @Override
        public <T> T measure(Supplier<T> r) {
            return r.get();
        }
    }
}
