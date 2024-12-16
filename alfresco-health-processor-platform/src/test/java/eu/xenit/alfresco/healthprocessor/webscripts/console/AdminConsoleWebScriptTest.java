package eu.xenit.alfresco.healthprocessor.webscripts.console;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminConsoleWebScriptTest {

    @Test
    void executeImpl() {
        ResponseViewRenderer renderer = mock(ResponseViewRendererImpl.class);

        AdminConsoleWebScript webScript = new AdminConsoleWebScript(renderer);

        Map<String, Object> model = webScript.executeImpl(null, null, null);
        assertThat(model, hasKey("healthprocessor"));

    }

}