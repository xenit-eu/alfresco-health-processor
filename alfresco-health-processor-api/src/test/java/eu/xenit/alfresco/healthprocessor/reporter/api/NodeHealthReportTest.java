package eu.xenit.alfresco.healthprocessor.reporter.api;

import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeHealthReportTest {

    @Test
    void ofHealthy() {
        Set<NodeRef> nodeRefs = Set.of(new NodeRef("workspace://SpacesStore/1234"), new NodeRef("workspace://SpacesStore/5678"));
        String[] messages = {"message1", "message2"};

        Set<NodeHealthReport> nodeHealthReports = NodeHealthReport.ofHealthy(nodeRefs, messages);
        assertEquals(2, nodeHealthReports.size());
        for (NodeHealthReport report : nodeHealthReports) {
            assertEquals(NodeHealthStatus.HEALTHY, report.getStatus());
            assertEquals(Set.of("message1", "message2"), report.getMessages());
        }
    }
}