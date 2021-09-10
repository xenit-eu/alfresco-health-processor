package eu.xenit.alfresco.healthprocessor.reporter.repository;


import org.alfresco.service.namespace.QName;

interface HealthProcessorModel {

    String HP_NAMESPACE = "http://www.xenit.eu/model/HealthProcessor";

    QName TYPE_REPORTS = QName.createQName(HP_NAMESPACE, "reports");
    QName PROP_CYCLE_ID = QName.createQName(HP_NAMESPACE, "cycleId");
    QName PROP_REPOSITORY_ID = QName.createQName(HP_NAMESPACE, "repositoryId");

    QName TYPE_REPORT = QName.createQName(HP_NAMESPACE, "report");
    QName PROP_PLUGIN = QName.createQName(HP_NAMESPACE, "plugin");

    QName ASPECT_CHUNK = QName.createQName(HP_NAMESPACE, "chunk");
    QName PROP_CHUNK_SEQUENCE = QName.createQName(HP_NAMESPACE, "chunkSequence");

    QName ASPECT_INCOMPLETE_REPORT = QName.createQName(HP_NAMESPACE, "incompleteReport");
    QName PROP_COMPLETION_PERCENTAGE = QName.createQName(HP_NAMESPACE, "completionPercentage");

    QName ASPECT_STATISTICS = QName.createQName(HP_NAMESPACE, "statistics");
    QName PROP_UNHEALTHY_NODE_COUNT = QName.createQName(HP_NAMESPACE, "unhealthyNodeCount");
    QName PROP_FIXED_NODE_COUNT = QName.createQName(HP_NAMESPACE, "fixedNodeCount");

    QName ASPECT_DETAILS = QName.createQName(HP_NAMESPACE, "details");
    QName PROP_UNHEALTHY_NODES = QName.createQName(HP_NAMESPACE, "unhealthyNodes");
    QName PROP_FIXED_NODES = QName.createQName(HP_NAMESPACE, "fixedNodes");
}
