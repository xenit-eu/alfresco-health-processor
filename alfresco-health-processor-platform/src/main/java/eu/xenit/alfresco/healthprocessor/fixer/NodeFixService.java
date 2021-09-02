package eu.xenit.alfresco.healthprocessor.fixer;

import eu.xenit.alfresco.healthprocessor.fixer.api.HealthFixerPlugin;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixReport;
import eu.xenit.alfresco.healthprocessor.fixer.api.NodeFixStatus;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class NodeFixService {

    private final List<HealthFixerPlugin> fixers;
    private final TransactionHelper transactionHelper;

    public Set<NodeHealthReport> fixUnhealthyNodes(Class<? extends HealthProcessorPlugin> pluginClass,
            Set<NodeHealthReport> nodeHealthReports) {
        Set<NodeHealthReport> unhealthyReports = nodeHealthReports.stream()
                .filter(healthReport -> healthReport.getStatus() == NodeHealthStatus.UNHEALTHY)
                .collect(Collectors.toSet());

        if (unhealthyReports.isEmpty()) {
            return nodeHealthReports;
        }

        Set<NodeFixReport> fixReports = fixNodes(pluginClass, unhealthyReports);

        Set<NodeHealthReport> revisedHealthReports = new HashSet<>(nodeHealthReports);

        Map<NodeHealthReport, Set<NodeFixReport>> fixReportsByHealthReport = fixReports.stream()
                .collect(Collectors.groupingBy(NodeFixReport::getHealthReport, Collectors.toSet()));

        for (Entry<NodeHealthReport, Set<NodeFixReport>> entry : fixReportsByHealthReport.entrySet()) {
            NodeHealthReport revisedReport = revisedHealthReport(entry.getKey(), entry.getValue());
            if (revisedReport != entry.getKey()) {
                log.debug("Health report {} was revised to {} thanks to fixes", entry.getKey(), revisedReport);
                revisedHealthReports.remove(entry.getKey());
                revisedHealthReports.add(revisedReport);
            }
        }

        return revisedHealthReports;
    }

    private Set<NodeFixReport> fixNodes(Class<? extends HealthProcessorPlugin> pluginClass,
            Set<NodeHealthReport> unhealthyNodes) {
        log.debug("Processing #{} unhealthy reports from plugin '{}'", unhealthyNodes.size(), pluginClass);
        log.trace("{}", unhealthyNodes);
        Set<NodeFixReport> allFixReports = new HashSet<>();
        for (HealthFixerPlugin fixer : fixers) {
            if (fixer.isEnabled()) {
                log.debug("Fixer '{}' will process #{} unhealthy reports", fixer.getClass(), unhealthyNodes.size());
                Set<NodeFixReport> fixReports = transactionHelper.inNewTransaction(
                        () -> fixer.fix(pluginClass, unhealthyNodes), false);
                allFixReports.addAll(fixReports);
            }
        }
        return allFixReports;
    }


    private static NodeHealthReport revisedHealthReport(NodeHealthReport healthReport, Set<NodeFixReport> fixReports) {
        Map<NodeFixStatus, Set<NodeFixReport>> reportsByStatus = fixReports.stream()
                .collect(Collectors.groupingBy(NodeFixReport::getFixStatus, Collectors.toSet()));

        int numFailed = reportsByStatus.getOrDefault(NodeFixStatus.FAILED, Collections.emptySet()).size();
        int numSucceeded = reportsByStatus.getOrDefault(NodeFixStatus.SUCCEEDED, Collections.emptySet()).size();

        if (log.isDebugEnabled()) {
            Map<NodeFixStatus, Integer> reportStats = reportsByStatus.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().size()));
            log.debug("Health report {} has NodeFixReports: {}", healthReport, reportStats);
        }
        log.trace("Fix reports [{}]", fixReports);

        healthReport.data(NodeFixReport.class).addAll(fixReports);
        // No failed fixes, some succeeded fixes
        if (numFailed == 0 && numSucceeded > 0) {
            return reviseHealthReportFixed(healthReport);
        } else {
            return healthReport;
        }
    }

    private static NodeHealthReport reviseHealthReportFixed(NodeHealthReport healthReport) {
        NodeHealthReport newReport = new NodeHealthReport(NodeHealthStatus.FIXED, healthReport.getNodeRef(),
                healthReport.getMessages());
        newReport.data(healthReport.data());
        return newReport;
    }

}
