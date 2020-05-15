package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Tracker {

    @Autowired
    private AlfrescoWebscriptClient client;

    @Autowired
    private ElasticsearchClient elasticClient;

    @Scheduled(cron = "*/10 * * * * *")
    public void track(){
        System.out.println("starting");
        Transactions transactions = client.getTransactions(1589452972235L,1589456572235L,2000,"workspace://SpacesStore");

        List<Long> transactionIds = new ArrayList<>();
        for(Transaction t : transactions.getTransactions()){
            System.out.println(t);
            transactionIds.add(t.getId());
        }

        List<Long> dbnodeIds = new ArrayList<>();
        for(Node node : client.getNodes(transactionIds)){
            System.out.println("n:"+node);
            dbnodeIds.add(node.getId());
        }

        GetNodeMetadataParam p = new GetNodeMetadataParam();
        p.setNodeIds(dbnodeIds);

        NodeMetadatas nmds = client.getNodeMetadata(p);

        for(NodeMetadata nmd : nmds.getNodes()){
            System.out.println(nmd);
        }


        System.out.println("finished:" + transactions.getTransactions().size());
    }
}
