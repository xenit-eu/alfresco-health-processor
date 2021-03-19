package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Value;

@Getter
public class ReportersView {

    List<Reporter> reporters;

    public ReportersView(List<HealthReporter> reporters) {
        this.reporters = toViewModel(reporters);
    }

    private static List<Reporter> toViewModel(List<HealthReporter> reporters) {
        if (reporters == null) {
            return null;
        }
        return reporters.stream()
                .map(ReportersView::toViewModel)
                .collect(Collectors.toList());
    }

    private static Reporter toViewModel(HealthReporter reporter) {
        return new Reporter(reporter.getClass().getSimpleName(), reporter.isEnabled());
    }

    @Value
    public static class Reporter {

        String name;
        boolean enabled;
    }

}
