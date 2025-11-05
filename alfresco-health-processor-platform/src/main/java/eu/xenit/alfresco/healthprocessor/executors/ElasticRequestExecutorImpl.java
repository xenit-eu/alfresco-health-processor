package eu.xenit.alfresco.healthprocessor.executors;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alfresco.encryption.AlfrescoKeyStore;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.node.Transaction;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpoint;
import eu.xenit.alfresco.healthprocessor.endpoint.elastic.ElasticEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs HTTP requests on a ElasticSearch {@link SearchEndpoint}
 */
@Slf4j
@RequiredArgsConstructor
public class ElasticRequestExecutorImpl implements ElasticRequestExecutor
{

    private final HttpClient httpClient;

    private final NodeDAO nodeDAO;

    public ElasticRequestExecutorImpl(NodeDAO nodeDAO, AlfrescoKeyStore sslKeyStore, AlfrescoKeyStore sslTrustStore)
    {
        this(SslHttpClientFactory.setupHttpClient(sslKeyStore, sslTrustStore), nodeDAO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElasticResult checkNodeIndexed(SearchEndpoint endpoint, Collection<Status> nodeStatuses) throws IOException
    {
        JsonNode response = retrieveDocuments(endpoint, nodeStatuses);

        ElasticResult elasticResult = new ElasticResult();

        Map<String, Status> statusById = nodeStatuses.stream().collect(Collectors.toMap(s -> s.getNodeRef().getId(), Function.identity()));
        Map<Long, Transaction> txnById = new HashMap<>();
        
        boolean expectPathsIndexed = endpoint instanceof ElasticEndpoint && ((ElasticEndpoint)endpoint).isExpectPathsIndexed();

        JsonNode docs = response.path("docs");
        StreamSupport.stream(docs.spliterator(), false).filter(JsonNode::isObject).forEach(o -> {
            String id = o.path("_id").asText();
            boolean found = o.path("found").asBoolean();
            Status expectedStatus = statusById.remove(id);
            boolean expectedInIndex = !expectedStatus.isDeleted();
            if (expectedInIndex)
            {
                Serializable indexControlIsIndexed = nodeDAO.getNodeProperty(expectedStatus.getDbId(), ContentModel.PROP_IS_INDEXED);
                expectedInIndex = !Boolean.FALSE.equals(indexControlIsIndexed);
            }

            if (found)
            {
                JsonNode source = o.path("_source");
                boolean isAlive = source.path("ALIVE").asBoolean();
                if (expectedInIndex && isAlive)
                {
                    long lastMetadataUpdate = source.path("METADATA_INDEXING_LAST_UPDATE").asLong();
                    Transaction txn = txnById.computeIfAbsent(expectedStatus.getDbTxnId(), nodeDAO::getTxnById);

                    if (lastMetadataUpdate <= txn.getCommitTimeMs())
                    {
                        elasticResult.getOutdated().add(expectedStatus);
                    }
                    else if (expectPathsIndexed && !source.has("PATH"))
                    {
                        elasticResult.getPathMissing().add(expectedStatus);
                    }
                    else
                    {
                        elasticResult.getFound().add(expectedStatus);
                    }
                }
                else if (expectedInIndex && !isAlive)
                {
                    log.trace("Node {} is indexed but not marked as alive", expectedStatus);
                    elasticResult.getMissing().add(expectedStatus);
                }
                else if (isAlive)
                {
                    log.trace("Node {} was deleted but is still in index and marked as alive", expectedStatus);
                    elasticResult.getSuperflous().add(expectedStatus);
                }
            }
            else if (expectedInIndex)
            {
                log.trace("Node {} is not yet indexed", expectedStatus);
                elasticResult.getMissing().add(expectedStatus);
            }
        });

        return elasticResult;
    }

    public ActionResponse updateDocumentAliveState(SearchEndpoint endpoint, String id, boolean alive) throws IOException
    {
        ActionResponse result = null;

        log.debug("Updating ALIVE to {} for {} on endpoint {}", alive, id, endpoint);
        String payload = "{\"detect_noop\":true,\"doc\":{\"ALIVE\":" + String.valueOf(alive) + "}}";
        HttpPost multiDocRequest = new HttpPost(endpoint.getBaseUri().resolve("./_update/" + id));
        try
        {
            multiDocRequest.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            log.trace("Executing HTTP request {}", multiDocRequest);

            JsonNode res = httpClient.execute(multiDocRequest, new JSONResponseHandler());
            String resultText = res.path("result").asText();
            boolean updated = "updated".equals(resultText);

            if (updated)
            {
                result = new ActionResponse(updated, "ALIVE flag updated");
            }
            else
            {
                result = new ActionResponse(false, "ALIVE flag not updated");
            }

            return result;
        }
        finally
        {
            multiDocRequest.reset();
        }
    }

    public ActionResponse deleteNodeIndexEntry(SearchEndpoint endpoint, String id) throws IOException
    {
        ActionResponse result = null;

        log.debug("Deleting node inde entry for {} on endpoint {}", id, endpoint);
        HttpDelete deleteRequest = new HttpDelete(endpoint.getBaseUri().resolve("./_doc/" + id));
        try
        {
            log.trace("Executing HTTP request {}", deleteRequest);

            JsonNode res = httpClient.execute(deleteRequest, new JSONResponseHandler());
            String resultText = res.path("result").asText();
            boolean deleted = "deleted".equals(resultText);

            if (deleted)
            {
                result = new ActionResponse(deleted, "Node index entry deleted");
            }
            else
            {
                result = new ActionResponse(false, "Node index entry not deleted");
            }

            return result;
        }
        finally
        {
            deleteRequest.reset();
        }
    }

    private JsonNode retrieveDocuments(SearchEndpoint endpoint, Collection<Status> nodeStatuses) throws IOException
    {
        log.debug("Retrieving {} index documents from endpoint {}", nodeStatuses.size(), endpoint);
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\"ids\": [");
        boolean first = true;
        for (Status status : nodeStatuses)
        {
            if (!first)
            {
                sb.append(',');
            }
            else
            {
                first = false;
            }

            sb.append('"').append(status.getNodeRef().getId()).append('"');
        }
        sb.append("]}");

        // problem 1: _source=true returns all fields + does not respect _source_excludes
        // problem 2: _source=field1,field2 does not support %-encoded fields
        // if we wanted to validate more than just the metadata index state (e.g. content metadata (size)), we'd have to retrieve full data
        String relativePath = "./_mget?_source=METADATA_INDEXING_LAST_UPDATE,ALIVE";
        if (endpoint instanceof ElasticEndpoint && ((ElasticEndpoint)endpoint).isExpectPathsIndexed())
        {
            relativePath += ",PATH";
        }
        HttpPost multiDocRequest = new HttpPost(endpoint.getBaseUri().resolve(relativePath));
        try
        {
            multiDocRequest.setEntity(new StringEntity(sb.toString(), ContentType.APPLICATION_JSON));
            log.trace("Executing HTTP request {}", multiDocRequest);
            return httpClient.execute(multiDocRequest, new JSONResponseHandler());
        }
        finally
        {
            multiDocRequest.reset();
        }
    }

}
