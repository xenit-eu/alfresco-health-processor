package eu.xenit.alfresco.healthprocessor.processing;

import eu.xenit.alfresco.healthprocessor.indexing.AssertIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessorServiceTest {

    private AssertTransactionHelper transactionHelper;
    private AssertHealthProcessorPlugin processorPlugin;
    private AssertIndexingStrategy indexingStrategy;

    private ProcessorServiceBuilder builder;

    @BeforeEach
    void setup() {
        transactionHelper = new AssertTransactionHelper();
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
                .plugins(Collections.emptySet())
                .build()
                .execute();

        indexingStrategy.expectResetInvocations(0);
        indexingStrategy.expectGetNextNodeIdsInvocations(0);
        processorPlugin.expectNoInvocation();
    }

    @Test
    void execute() {
        indexingStrategy.nextAnswer(TestNodeRefs.REFS[0], TestNodeRefs.REFS[1]);
        builder
                .build()
                .execute();

        indexingStrategy.expectResetInvocations(1);
        indexingStrategy.expectGetNextNodeIdsInvocations(2);
        processorPlugin.expectInvocation(TestNodeRefs.REFS[1], TestNodeRefs.REFS[0]);
    }

    private ProcessorService service() {
        return service(ProcConfigUtil.defaultConfig());
    }

    private ProcessorService service(ProcessorConfiguration config) {
        return new ProcessorService(config, indexingStrategy, transactionHelper, Collections.emptySet(),
                Collections.emptySet());
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
        private Set<HealthProcessorPlugin> plugins;
        private Set<HealthReporter> reporters;

        ProcessorServiceBuilder plugin(HealthProcessorPlugin plugin) {
            if (plugins == null) {
                plugins = new HashSet<>();
            }
            plugins.add(plugin);
            return this;
        }

        ProcessorServiceBuilder reporter(HealthReporter reporter) {
            if (reporters == null) {
                reporters = new HashSet<>();
            }
            reporters.add(reporter);
            return this;
        }

        ProcessorService build() {
            return new ProcessorService(config, indexingStrategy, transactionHelper, plugins, reporters);
        }
    }

}