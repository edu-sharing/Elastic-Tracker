package org.edu_sharing.elasticsearch.alfresco.client;

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
        String url = getUrl(URL_NODE_METADATA);
        NodeMetadatas nmds = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(param)).readEntity(NodeMetadatas.class);
        return nmds;
    }

    public List<NodeToIndex> getNodesToIndex(GetNodeMetadataParam param){
        NodeMetadatas nmds = getNodeMetadata(param);

        LinkedHashSet<Long> acls = new LinkedHashSet<Long>();
        for(NodeMetadata md : nmds.getNodes()){
          long aclId =  md.getAclId();
          acls.add(aclId);
        }
        GetReadersParam getReadersParam = new GetReadersParam();
        getReadersParam.setAclIds(new ArrayList<Long>(acls));
        ReadersACL readersACL = this.getReader(getReadersParam);

        List<NodeToIndex> result = new ArrayList<>();
        for(NodeMetadata node : nmds.getNodes()){
            for(Reader reader : readersACL.getAclsReaders()){
                if(node.getAclId() == reader.aclId){
                    NodeToIndex nti = new NodeToIndex();
                    nti.setNodeMetadata(node);
                    nti.setReader(reader);
                    result.add(nti);
                }
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






    private String getUrl(String path){
        return alfrescoProtocol+"://"+alfrescoHost+":"+alfrescoPort+path;
    }
}
