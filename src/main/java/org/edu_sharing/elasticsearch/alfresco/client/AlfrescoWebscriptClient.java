package org.edu_sharing.elasticsearch.alfresco.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

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


    private Client client = ClientBuilder.newClient();


    public List<Node> getNodes(List<Long> transactionIds ){
        return null;
    }

    public Transactions getTransactions(long fromCommitTime, long toCommitTime, int maxResults, String stores){


        String url = getUrl(URL_TRANSACTIONS);

        Transactions transactions = client
                .target(url).queryParam("fromCommitTime",fromCommitTime)
                .queryParam("toCommitTime",toCommitTime)
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
