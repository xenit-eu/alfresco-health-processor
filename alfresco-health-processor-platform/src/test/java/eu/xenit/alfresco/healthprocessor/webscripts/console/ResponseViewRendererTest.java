package eu.xenit.alfresco.healthprocessor.webscripts.console;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.xenit.alfresco.healthprocessor.indexing.FakeTrackingComponent;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingConfiguration;
import eu.xenit.alfresco.healthprocessor.indexing.IndexingStrategy;
import eu.xenit.alfresco.healthprocessor.indexing.txnid.TxnIdBasedIndexingStrategy;
import eu.xenit.alfresco.healthprocessor.plugins.api.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.plugins.api.HealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.processing.ProcessorService;
import eu.xenit.alfresco.healthprocessor.processing.ProcessorState;
import eu.xenit.alfresco.healthprocessor.reporter.SummaryLoggingHealthReporter;
import eu.xenit.alfresco.healthprocessor.reporter.api.HealthReporter;
import eu.xenit.alfresco.healthprocessor.util.InMemoryAttributeHelper;
import eu.xenit.alfresco.healthprocessor.webscripts.console.model.AdminConsoleResponseView;
import java.util.Collections;
import org.alfresco.repo.module.ModuleVersionNumber;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.junit.jupiter.api.Test;

class ResponseViewRendererTest {

    private static final String VERSION = "0.0.7";

    @Test
    void render() {
        ResponseViewRenderer renderer = new ResponseViewRenderer();

        ModuleDetails moduleDetails = mock(ModuleDetails.class);
        when(moduleDetails.getModuleVersionNumber()).thenReturn(new ModuleVersionNumber(VERSION));
        renderer.setModuleDetails(moduleDetails);

        ProcessorService processorService = mock(ProcessorService.class);
        when(processorService.getState()).thenReturn(ProcessorState.ACTIVE);
        renderer.setProcessorService(processorService);

        IndexingConfiguration indexingConfiguration = new IndexingConfiguration("txn-id", 10, 20, 2);
        renderer.setIndexingConfiguration(indexingConfiguration);

        IndexingStrategy indexingStrategy = new TxnIdBasedIndexingStrategy(indexingConfiguration,
                new FakeTrackingComponent(), new InMemoryAttributeHelper());
        renderer.setIndexingStrategy(indexingStrategy);

        HealthProcessorPlugin plugin = new AssertHealthProcessorPlugin();
        renderer.setPlugins(Collections.singletonList(plugin));

        HealthReporter reporter = new SummaryLoggingHealthReporter();
        renderer.setReporters(Collections.singletonList(reporter));

        AdminConsoleResponseView view = renderer.renderView();
        assertThat(view, is(notNullValue()));
        assertThat(view.getVersion(), is(equalTo(VERSION)));
        assertThat(view.getStatus(), is(equalTo("ACTIVE")));
        assertThat(view.getIndexing(), is(notNullValue()));
        assertThat(view.getIndexing().getId(), is("txn-id"));
        assertThat(view.getIndexing().getConfiguration().keySet(), is(not(empty())));

        assertThat(view.getPlugins(), is(notNullValue()));
        assertThat(view.getPlugins().getPlugins(), hasSize(1));
        assertThat(view.getPlugins().getPlugins().get(0).getName(), is(equalTo("AssertHealthProcessorPlugin")));

        assertThat(view.getReporters(), is(notNullValue()));
        assertThat(view.getReporters().getReporters(), hasSize(1));
        assertThat(view.getReporters().getReporters().get(0).getName(), is(equalTo("SummaryLoggingHealthReporter")));

    }

}