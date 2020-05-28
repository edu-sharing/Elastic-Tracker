package org.edu_sharing.elasticsearch.alfresco.client;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;

@Component
public class AlfrescoWebscriptClient {

    @Value("${alfresco.host}")
    String alfrescoHost;

    @Value("${alfresco.port}")
    String alfrescoPort;

    @Value("${alfresco.protocol}")
    String alfrescoProtocol;

    String URL_TRANSACTIONS = "/alfresco/service/api/solr/transactions";

    String URL_NODES_TRANSACTION = "/alfresco/s/api/solr/nodes";

    String URL_NODE_METADATA = "/alfresco/s/api/solr/metadata";

    String URL_ACL_READERS ="/alfresco/s/api/solr/aclsReaders";

    String URL_ACL_CHANGESETS = "/alfresco/s/api/solr/aclchangesets";

    String URL_ACLS = "/alfresco/s/api/solr/acls";

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(AlfrescoWebscriptClient.class);

    private Client client;



    public AlfrescoWebscriptClient(){
        Logger logger = Logger.getLogger(getClass().getName());
        Feature feature = new LoggingFeature(logger, Level.FINEST, null, null);

        client = ClientBuilder.newBuilder().register(feature).build();
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

    public List<NodeData> getNodeData(List<Node> nodes){

        List<Long> dbnodeids = new ArrayList<>();
        for(Node node : nodes){
            dbnodeids.add(node.getId());
        }

        GetNodeMetadataParam getNodeMetadataParam = new GetNodeMetadataParam();
        getNodeMetadataParam.setNodeIds(dbnodeids);

        NodeMetadatas nmds = getNodeMetadata(getNodeMetadataParam);

        LinkedHashSet<Long> acls = new LinkedHashSet<Long>();
        for(NodeMetadata md : nmds.getNodes()){
          long aclId =  md.getAclId();
          acls.add(aclId);
        }
        GetReadersParam getReadersParam = new GetReadersParam();
        getReadersParam.setAclIds(new ArrayList<Long>(acls));
        ReadersACL readersACL = this.getReader(getReadersParam);

        List<NodeData> result = new ArrayList<>();
        for(NodeMetadata nodeMetadata : nmds.getNodes()){

            Node node = null;

            for(Node n : nodes){
                if(n.getId() == nodeMetadata.getId()){
                    node = n;
                }
            }

            for(Reader reader : readersACL.getAclsReaders())
                if (nodeMetadata.getAclId() == reader.aclId) {
                    NodeData nodeData = new NodeData();
                    nodeData.setNodeMetadata(nodeMetadata);
                    nodeData.setReader(reader);
                    nodeData.setNode(node);

                    result.add(nodeData);
                }
        }

        return result;
    }

    public ReadersACL getReader(GetReadersParam param){
        String url = getUrl(URL_ACL_READERS);
        ReadersACL readers = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param)).readEntity(ReadersACL.class);

        return readers;
    }


    public Transactions getTransactions(Long minTxnId, Long maxTxnId, Long fromCommitTime, Long toCommitTime, int maxResults, String stores){


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
                .queryParam("stores",stores)
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






    private String getUrl(String path){
        return alfrescoProtocol+"://"+alfrescoHost+":"+alfrescoPort+path;
    }
}
