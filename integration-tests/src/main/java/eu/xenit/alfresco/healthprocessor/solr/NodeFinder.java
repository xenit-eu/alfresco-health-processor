package eu.xenit.alfresco.healthprocessor.solr;

import java.util.List;
import java.util.stream.Collectors;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;

public class NodeFinder {

    private final NodeService nodeService;
    private final SearchService searchService;
    private final NamespaceService namespaceService;

    private static final String XPATH = "/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files/cm:Images/*";

    public NodeFinder(NodeService nodeService, SearchService searchService,
            NamespaceService namespaceService) {
        this.nodeService = nodeService;
        this.searchService = searchService;
        this.namespaceService = namespaceService;
    }

    public List<NodeRef.Status> findNodes() {
        NodeRef rootNode = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

        List<NodeRef> nodeRefs = searchService.selectNodes(rootNode, XPATH, null, namespaceService, false);

        return nodeRefs.stream().map(nodeService::getNodeStatus).collect(Collectors.toList());
    }

    public List<NodeRef> findWithSolr() {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        searchParameters.setQueryConsistency(QueryConsistency.EVENTUAL);
        searchParameters.setQuery("PATH:'" + XPATH + "'");
        ResultSet resultSet = searchService.query(searchParameters);
        return resultSet.getNodeRefs();
    }
}
