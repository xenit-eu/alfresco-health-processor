package eu.xenit.alfresco.healthprocessor.reporter.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.CycleProgress;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthReport;
import eu.xenit.alfresco.healthprocessor.reporter.api.NodeHealthStatus;
import eu.xenit.alfresco.healthprocessor.reporter.api.ProcessorPluginOverview;
import eu.xenit.alfresco.healthprocessor.reporter.api.ToggleableHealthReporter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

@RequiredArgsConstructor
@Slf4j
public class RepositoryFileHealthReporter extends ToggleableHealthReporter {

    private final RepositoryReportManager reportManager;
    private final NodeService nodeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<Class<? extends HealthProcessorPlugin>, AtomicInteger> sequenceNumber = new HashMap<>();
    private NodeRef reportFolder;

    @Override
    public void onStart() {
        reportFolder = reportManager.createNewReport(UUID.randomUUID().toString());
        log.info("Created new report folder at {}", reportFolder);
        sequenceNumber = new HashMap<>();
        reportManager.removeExpiredReports();
    }

    @Override
    public void processReports(@Nonnull Class<? extends HealthProcessorPlugin> pluginClass,
            @Nonnull Set<NodeHealthReport> reports) {
        ContentWriter writer = reportManager.createNewReportChunk(reportFolder, pluginClass,
                sequenceNumber.computeIfAbsent(pluginClass, _unused -> new AtomicInteger(0)));

        try (OutputStream outputStream = writer.getContentOutputStream()) {
            objectMapper
                    .writerFor(NodeHealthReport.class)
                    .withRootValueSeparator("\n")
                    .writeValues(outputStream)
                    .writeAll(reports);
            // Write terminating newline to the file
            outputStream.write('\n');
        } catch (IOException e) {
            log.error("Failed to write a report chunk", e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onCycleDone(@Nonnull List<ProcessorPluginOverview> overviews) {
        for (ProcessorPluginOverview overview : overviews) {
            Class<? extends HealthProcessorPlugin> plugin = overview.getPluginClass();
            NodeRef aggregate = reportManager.createNewReportAggregate(reportFolder, plugin);
            nodeService.setProperty(aggregate, HealthProcessorModel.PROP_UNHEALTHY_NODES,
                    extractNodeRefsOfStatus(overview, NodeHealthStatus.UNHEALTHY));
            nodeService.setProperty(aggregate, HealthProcessorModel.PROP_FIXED_NODES,
                    extractNodeRefsOfStatus(overview, NodeHealthStatus.FIXED));
        }
        reportManager.finishReport(reportFolder);
        reportFolder = null;
    }

    @Override
    public void onProgress(@Nonnull CycleProgress progress) {
        reportManager.updateProgress(reportFolder, progress.getProgress());
    }

    private static ArrayList<NodeRef> extractNodeRefsOfStatus(ProcessorPluginOverview processorPluginOverview,
            NodeHealthStatus nodeHealthStatus) {
        return processorPluginOverview.getReports()
                .stream()
                .filter(healthReport -> healthReport.getStatus() == nodeHealthStatus)
                .map(NodeHealthReport::getNodeRef).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Map<String, String> getConfiguration() {
        Map<String, String> configuration = new HashMap<>();
        configuration.putAll(super.getConfiguration());
        configuration.putAll(reportManager.getConfiguration());
        return configuration;
    }

    @Override
    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<>();
        state.put("reportFolder", Objects.toString(reportFolder));

        sequenceNumber.forEach((pluginClass, value) -> {
            state.put("sequenceNumber." + pluginClass.getName(), value.toString());
        });
        return state;
    }
}
