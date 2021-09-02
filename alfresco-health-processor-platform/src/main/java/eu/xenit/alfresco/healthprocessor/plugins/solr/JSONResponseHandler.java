package eu.xenit.alfresco.healthprocessor.plugins.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class JSONResponseHandler implements ResponseHandler<JsonNode> {

    @Override
    public JsonNode handleResponse(final HttpResponse response)
            throws IOException {
        // This is copy-paste from AbstractResponseHandler,
        // The versions on the classpath of httpclient & httpcore are not compatible with each other
        // so lombok is unable to properly compile if we extend from AbstractResponseHandler
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
            EntityUtils.consume(entity);
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }
        return entity == null ? null : handleEntity(entity);
    }

    public JsonNode handleEntity(HttpEntity entity) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(entity.getContent());
    }
}
