package eu.xenit.alfresco.healthprocessor.solr;

import com.fasterxml.jackson.databind.node.BooleanNode;

import java.io.IOException;

import org.alfresco.service.cmr.repository.NodeRef;

import eu.xenit.alfresco.healthprocessor.endpoint.SearchEndpointSelector;
import eu.xenit.alfresco.healthprocessor.endpoint.solr.SolrEndpoint;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrNodeCommand;
import eu.xenit.alfresco.healthprocessor.executors.SolrRequestExecutor.SolrActionResponse;
import eu.xenit.alfresco.healthprocessor.index.NodeFinder;

public class SolrPurgeNodeWebScript extends SolrNodeHandlerWebScript {

    public SolrPurgeNodeWebScript(NodeFinder nodeFinder,
                                  SearchEndpointSelector<SolrEndpoint> endpointSelector,
                                  SolrRequestExecutor solrRequestExecutor) {
        super(nodeFinder, endpointSelector, solrRequestExecutor);
    }

    @Override
    protected BooleanNode handleNode(SolrEndpoint endpoint, NodeRef.Status nodeStatus) throws IOException {
        SolrActionResponse actionResponse = solrRequestExecutor.executeAsyncNodeCommand(endpoint, nodeStatus, SolrNodeCommand.PURGE);
        return BooleanNode.valueOf(
                actionResponse.isSuccessFull());
    }
}
