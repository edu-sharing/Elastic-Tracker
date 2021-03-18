package org.edu_sharing.elasticsearch.alfresco.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AlfrescoWebscriptClient {

    @Value("${alfresco.host}")
    String alfrescoHost;

    @Value("${alfresco.port}")
    String alfrescoPort;

    @Value("${alfresco.protocol}")
    String alfrescoProtocol;

    @Value("${log.requests}")
    String logRequests;

    String URL_TRANSACTIONS = "/alfresco/service/api/solr/transactions";

    String URL_NODES_TRANSACTION = "/alfresco/s/api/solr/nodes";

    String URL_NODE_METADATA = "/alfresco/s/api/solr/metadata";

    String URL_ACL_READERS ="/alfresco/s/api/solr/aclsReaders";

    String URL_ACL_CHANGESETS = "/alfresco/s/api/solr/aclchangesets";

    String URL_ACLS = "/alfresco/s/api/solr/acls";

    String URL_CONTENT = "/alfresco/s/api/solr/textContent";

    String URL_PERMISSIONS = "/alfresco/service/api/solr/permissions";

    private static final Logger logger = LoggerFactory.getLogger(AlfrescoWebscriptClient.class);

    private Client client;



    public AlfrescoWebscriptClient() {
        client = ClientBuilder.newBuilder()
                .register(JacksonJsonProvider.class).build();
        //client.property("use.async.http.conduit", Boolean.TRUE);
        //client.property("org.apache.cxf.transport.http.async.usePolicy", AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);
        if (Boolean.parseBoolean(logRequests)) {
            client.register(new LoggingFeature());
        }
    }

    public List<Node> getNodes(List<Long> transactionIds ){

        String url = getUrl(URL_NODES_TRANSACTION);

        GetNodeParam p = new GetNodeParam();
        p.setTxnIds(transactionIds);

        Nodes node = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(p)).readEntity(Nodes.class);

        return node.getNodes();
    }

    public String getTextContent(Long dbid){
        String url = getUrl(URL_CONTENT);
        url += "?nodeId="+dbid;
        return client.target(url)
                .request(MediaType.TEXT_PLAIN)
                .get().readEntity(String.class);
    }

    public NodeMetadatas getNodeMetadata(GetNodeMetadataParam param){
        return this.getNodeMetadata(param,false);
    }
    public NodeMetadatas getNodeMetadata(GetNodeMetadataParam param, boolean debug){
        String url = getUrl(URL_NODE_METADATA);
        Response resp = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param));

        if(debug){
            String valueAsString = resp.readEntity(String.class);
            logger.error("problems with node(s):" + valueAsString);
            return null;
        }else{
            NodeMetadatas nmds = resp.readEntity(NodeMetadatas.class);
            return nmds;
        }
    }
    public List<NodeMetadata> getNodeMetadata(List<Node> nodes){

        List<Long> dbnodeids = new ArrayList<>();
        for(Node node : nodes){
            dbnodeids.add(node.getId());
        }

        GetNodeMetadataParam getNodeMetadataParam = new GetNodeMetadataParam();
        getNodeMetadataParam.setNodeIds(dbnodeids);

        NodeMetadatas nmds = getNodeMetadata(getNodeMetadataParam);
        return nmds.getNodes();

    }
    public List<NodeData> getNodeData(List<NodeMetadata> nodes){

        if(nodes == null || nodes.size() == 0){
            return new ArrayList<>();
        }

        LinkedHashSet<Long> acls = new LinkedHashSet<>();
        for(NodeMetadata md : nodes){
          long aclId =  md.getAclId();
          acls.add(aclId);
        }
        GetPermissionsParam getPermissionsParam = new GetPermissionsParam();
        getPermissionsParam.setAclIds(new ArrayList<Long>(acls));
        ReadersACL readersACL = this.getReader(getPermissionsParam);
        AccessControlLists permissions = this.getAccessControlLists(getPermissionsParam);

        Map<Long, AccessControlList> permissionsMap = permissions.getAccessControlLists().stream()
                .collect(Collectors.toMap(AccessControlList::getAclId, accessControlList -> accessControlList));

        List<NodeData> result = new ArrayList<>();
        for(NodeMetadata nodeMetadata : nodes){

            NodeMetadata node = null;

            for(Reader reader : readersACL.getAclsReaders()) {
                if (nodeMetadata.getAclId() == reader.aclId) {
                    NodeData nodeData = new NodeData();
                    nodeData.setNodeMetadata(nodeMetadata);
                    nodeData.setReader(reader);
                    nodeData.setAccessControlList(permissionsMap.get(nodeMetadata.getAclId()));
                    result.add(nodeData);
                }
            }

        }

        for(NodeData nodeData : result){
            String fullText = getTextContent(nodeData.getNodeMetadata().getId());
            if(fullText != null) nodeData.setFullText(fullText);

            if("ccm:io".equals(nodeData.getNodeMetadata().getType())){
                List<Node> children = new ArrayList<>();
                if(nodeData.getNodeMetadata().getChildIds() != null) {
                    for (Long dbid : nodeData.getNodeMetadata().getChildIds()) {
                        Node childNode = new Node();
                        childNode.setId(dbid);
                        children.add(childNode);
                    }

                    if (children.size() > 0) {
                        nodeData.getChildren().addAll(getNodeData(this.getNodeMetadata(children)));
                    }
                }
            }
        }

        return result;
    }

    public ReadersACL getReader(GetPermissionsParam param){
        String url = getUrl(URL_ACL_READERS);
        ReadersACL readers = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param)).readEntity(ReadersACL.class);
        return readers;
    }


    public Transactions getTransactions(Long minTxnId, Long maxTxnId, Long fromCommitTime, Long toCommitTime, int maxResults){


        String url = getUrl(URL_TRANSACTIONS);

        String fromParam = "minTxnId";
        String toParam = "maxTxnId";
        Long fromValue = minTxnId;
        Long toValue = maxTxnId;
        if(fromCommitTime != null && fromCommitTime > -1){
            fromParam = "fromCommitTime";
            toParam = "toCommitTime";
            fromValue = fromCommitTime;
            toValue = toCommitTime;
        }

        Transactions transactions = client
                .target(url)
                .queryParam(fromParam,fromValue)
                .queryParam(toParam,toValue)
                .queryParam("maxResults",maxResults)
                .request(MediaType.APPLICATION_JSON)
                .get(Transactions.class);

        return transactions;
    }

    public AclChangeSets getAclChangeSets(Long fromId, Long toId, int maxResults){
        String url = getUrl(URL_ACL_CHANGESETS);
        AclChangeSets result = client.target(url)
                .queryParam("fromId",fromId)
                .queryParam("toId",toId)
                .queryParam("maxResults",maxResults)
                .request(MediaType.APPLICATION_JSON)
                .get(AclChangeSets.class);
        return result;
    }

    public Acls getAcls(GetAclsParam param){
        String url = getUrl(URL_ACLS);
        Acls readers = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param)).readEntity(Acls.class);

        return readers;
    }


    public AccessControlLists getAccessControlLists(GetPermissionsParam param){
        String url = getUrl(URL_PERMISSIONS);
        AccessControlLists accessControlLists = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param)).readEntity(AccessControlLists.class);
        return accessControlLists;
    }






    private String getUrl(String path){
        return alfrescoProtocol+"://"+alfrescoHost+":"+alfrescoPort+path;
    }
}
