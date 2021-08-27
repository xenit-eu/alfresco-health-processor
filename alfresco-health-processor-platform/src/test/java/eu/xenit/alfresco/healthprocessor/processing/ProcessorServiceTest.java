package eu.xenit.alfresco.healthprocessor.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import eu.xenit.alfresco.healthprocessor.indexing.AssertIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.reporter.ReportsService;
import eu.xenit.alfresco.healthprocessor.util.AssertTransactionHelper;
import eu.xenit.alfresco.healthprocessor.util.TestNodeRefs;
import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.alfresco.repo.cache.MemoryCache;
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
        ReportsService reportsService = mock(ReportsService.class);
        builder = ProcessorServiceBuilder.create()
                .config(ProcConfigUtil.defaultConfig())
                .indexingStrategy(indexingStrategy)
                .reportsService(reportsService)
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
        ProcessorService processorService = builder.build();

        assertThat(processorService.getState(), is(ProcessorState.IDLE));
        processorService.execute();
        assertThat(processorService.getState(), is(ProcessorState.IDLE));

        indexingStrategy.expectOnStartInvocation(1);
        indexingStrategy.expectGetNextNodeIdsInvocations(2);
        processorPlugin.expectInvocation(TestNodeRefs.REFS[1], TestNodeRefs.REFS[0]);
    }

    @Test
    void execute_indexingStrategyThrowsException() {
        indexingStrategy.nextThrow(new RuntimeException("Hammertime"));
        ProcessorService processorService = builder.build();
        assertThrows(RuntimeException.class, processorService::execute, "Hammertime");
        assertThat(processorService.getState(), is(ProcessorState.FAILED));
    }

    @Test
    void execute_withoutRateLimit() {
        int numberOfNodes = 10;
        indexingStrategy.nextAnswer(Arrays.copyOfRange(TestNodeRefs.REFS, 0, numberOfNodes));

        long startTime = System.currentTimeMillis();
        ProcessorService unlimitedService = builder
                .config(new ProcessorConfiguration(true, 1, -1, true, "Superman"))
                .build();
        unlimitedService.execute();
        long durationMs = System.currentTimeMillis() - startTime;

        assertThat(durationMs, is(lessThan(1000L)));
    }

    @Test
    void execute_withRateLimit() {
        int numberOfNodes = 10;
        indexingStrategy.nextAnswer(Arrays.copyOfRange(TestNodeRefs.REFS, 0, numberOfNodes));

        long startTime = System.currentTimeMillis();
        ProcessorService unlimitedService = builder
                .config(new ProcessorConfiguration(true, 1, 2, true, "Superman"))
                .build();
        unlimitedService.execute();
        long durationMs = System.currentTimeMillis() - startTime;

        assertThat(durationMs, is(greaterThan(4000L)));
        assertThat(durationMs, is(lessThan(6000L)));
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
        private ReportsService reportsService;

        ProcessorServiceBuilder plugin(HealthProcessorPlugin plugin) {
            if (plugins == null) {
                plugins = new ArrayList<>();
            }
            plugins.add(plugin);
            return this;
        }

        ProcessorService build() {
            return new ProcessorService(config, indexingStrategy, transactionHelper, plugins, reportsService,
                    new StateCache(new MemoryCache<>()));
        }
    }

}