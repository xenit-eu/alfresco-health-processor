package eu.xenit.alfresco.healthprocessor.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

public class MicrometerMetricFactory implements MetricFactory {

    @Override
    public TimerMetric createTimer(String name, String... tags) {
        return new MicrometerTimerMetric(Timer.builder(name)
                .tags(tags)
                .register(Metrics.globalRegistry));
    }

    @AllArgsConstructor
    private static class MicrometerTimerMetric implements TimerMetric {

        private final Timer timer;

        public void measure(Runnable r) {
            timer.record(r);
        }

        public <T> T measure(Supplier<T> r) {
            return timer.record(r);
        }

    }
}
