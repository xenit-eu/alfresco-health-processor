package eu.xenit.alfresco.healthprocessor.processing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.xenit.alfresco.healthprocessor.indexing.AssertIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessorServiceTest {

    private AssertHealthProcessorPlugin processorPlugin;
    private AssertIndexingStrategy indexingStrategy;

    private ProcessorServiceBuilder builder;

    @BeforeEach
    void setup() {
        AssertTransactionHelper transactionHelper = new AssertTransactionHelper();
        processorPlugin = new AssertHealthProcessorPlugin();
        indexingStrategy = new AssertIndexingStrategy();
        builder = ProcessorServiceBuilder.create()
                .config(ProcConfigUtil.defaultConfig())
                .indexingStrategy(indexingStrategy)
                .transactionHelper(transactionHelper)
                .plugin(processorPlugin);
    }

    @Test
    void execute_noPluginsAvailable_indexingNotStarted() {
        builder
                .plugins(null)
                .build()
                .execute();
        builder
                .plugins(Collections.emptyList())
                .build()
                .execute();

        indexingStrategy.expectOnStartInvocation(0);
        indexingStrategy.expectGetNextNodeIdsInvocations(0);
        processorPlugin.expectNoInvocation();
    }

    @Test
    void execute() {
        indexingStrategy.nextAnswer(TestNodeRefs.REFS[0], TestNodeRefs.REFS[1]);
        builder
                .build()
                .execute();

        indexingStrategy.expectOnStartInvocation(1);
        indexingStrategy.expectGetNextNodeIdsInvocations(2);
        processorPlugin.expectInvocation(TestNodeRefs.REFS[1], TestNodeRefs.REFS[0]);
    }

    @Test
    void execute_pluginThrowsException() {
        indexingStrategy.nextThrow(new RuntimeException("Hammertime"));
        ProcessorService processorService = builder.build();
        assertThrows(RuntimeException.class, processorService::execute, "Hammertime");
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    private static class ProcessorServiceBuilder {

        static ProcessorServiceBuilder create() {
            return new ProcessorServiceBuilder();
        }

        private ProcessorConfiguration config;
        private IndexingStrategy indexingStrategy;
        private TransactionHelper transactionHelper;
        private List<HealthProcessorPlugin> plugins;
        private List<HealthReporter> reporters;

        ProcessorServiceBuilder plugin(HealthProcessorPlugin plugin) {
            if (plugins == null) {
                plugins = new ArrayList<>();
            }
            plugins.add(plugin);
            return this;
        }

        ProcessorServiceBuilder reporter(HealthReporter reporter) {
            if (reporters == null) {
                reporters = new ArrayList<>();
            }
            reporters.add(reporter);
            return this;
        }

        ProcessorService build() {
            return new ProcessorService(config, indexingStrategy, transactionHelper, plugins, reporters);
        }
    }

}