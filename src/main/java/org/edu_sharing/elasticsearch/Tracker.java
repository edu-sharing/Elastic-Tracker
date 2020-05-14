package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.alfresco.client.AlfrescoWebscriptClient;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.Transaction;
import org.edu_sharing.elasticsearch.alfresco.client.Transactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Tracker {

    @Autowired
    private AlfrescoWebscriptClient client;

    @Scheduled(cron = "*/10 * * * * *")
    public void track(){
        System.out.println("starting");
        Transactions transactions = client.getTransactions(1589452972235L,1589456572235L,2000,"workspace://SpacesStore");

        List<Long> transactionIds = new ArrayList<>();
        for(Transaction t : transactions.getTransactions()){
            System.out.println(t);
            transactionIds.add(t.getId());
        }

        for(Node node : client.getNodes(transactionIds)){
            System.out.println("n:"+node);
        }

        System.out.println("finished:" + transactions.getTransactions().size());
    }
}
