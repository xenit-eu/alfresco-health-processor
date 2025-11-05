package eu.xenit.alfresco.healthprocessor.webscripts.console.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import eu.xenit.alfresco.healthprocessor.checker.AssertHealthProcessorPlugin;
import eu.xenit.alfresco.healthprocessor.extensibility.BaseExtension;
import eu.xenit.alfresco.healthprocessor.fixer.AssertHealthFixerPlugin;

class ExtensionsViewTest {

    @Test
    void extensionView() {
        ExtensionsView extensionsView = new ExtensionsView(Arrays.asList(
                new AssertHealthProcessorPlugin(),
                new AssertHealthFixerPlugin()
        ));

        assertEquals(Arrays.asList(
                new ExtensionsView.Extension("AssertHealthProcessorPlugin", Collections.singletonMap("enabled", "true"), Collections.emptyMap()),
                new ExtensionsView.Extension("AssertHealthFixerPlugin", Collections.singletonMap("enabled", "true"), Collections.emptyMap())
        ), extensionsView.getExtensions());
    }

    @Test
    void extensionThatThrows() {
        BaseExtension fakeExtension = Mockito.mock(BaseExtension.class);

        RuntimeException someException = new RuntimeException("Something is broken?!");
        Mockito.doThrow(someException).when(fakeExtension).getState();
        Mockito.doThrow(someException).when(fakeExtension).getConfiguration();

        ExtensionsView extensionsView = new ExtensionsView(Collections.singletonList(fakeExtension));

        ExtensionsView.Extension extension = extensionsView.getExtensions().get(0);

        assertThat(extension.getConfiguration(), hasValue(someException.toString()));
        assertThat(extension.getState(), hasValue(someException.toString()));
    }
}