package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.alfresco.client.AlfrescoWebscriptClient;
import org.edu_sharing.elasticsearch.alfresco.client.Transaction;
import org.edu_sharing.elasticsearch.alfresco.client.Transactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Tracker {

    @Autowired
    private AlfrescoWebscriptClient webscriptClient;

    @Scheduled(cron = "*/10 * * * * *")
    public void track(){
        System.out.println("starting");
        Transactions transactions = webscriptClient.getTransactions(1589452972235L,1589456572235L,2000,"workspace://SpacesStore");

        for(Transaction t : transactions.getTransactions()){
            System.out.println(t);
        }

        System.out.println("finished:" + transactions.getTransactions().size());
    }
}
