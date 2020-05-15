package org.edu_sharing.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class Tracker {

    @Autowired
    private AlfrescoWebscriptClient client;

    @Autowired
    private ElasticsearchClient elasticClient;

    long lastIndexTime = -1;
    long lastTransactionId = -1;

    final static int maxResults = 50;

    //in ms
    final static int nextTimeStep = 3600000;

    final static String storeWorkspace = "workspace://SpacesStore";

    Logger logger = LogManager.getLogger(Tracker.class);

    @Scheduled(cron = "*/5 * * * * *")
    public void track(){
        logger.info("starting lastTransactionId:" +lastTransactionId+ " lastIndexTime:" + lastIndexTime +" " +  new Date(lastIndexTime));
        //Transactions transactions = client.getTransactions(1589452972235L,1589456572235L,2000,"workspace://SpacesStore");

        long toCommitTime = lastIndexTime + Tracker.nextTimeStep;
        Transactions transactions = (lastIndexTime < 1)
                ? client.getTransactions(0L,2000L,null,null, 1, Tracker.storeWorkspace)
                : client.getTransactions(null, null,lastIndexTime,toCommitTime, Tracker.maxResults, Tracker.storeWorkspace );



        if(transactions.getTransactions().size() == 0){

            logger.info("start   : step forward in time to find next transaction from:" + lastIndexTime +" to:" + toCommitTime + " max:"+ transactions.getMaxTxnCommitTime());
            do{

                lastIndexTime += Tracker.nextTimeStep;
                toCommitTime = lastIndexTime + Tracker.nextTimeStep;
                transactions = client.getTransactions(null,null,lastIndexTime, toCommitTime, Tracker.maxResults, Tracker.storeWorkspace);
            }while(transactions.getTransactions().size() == 0 && toCommitTime < transactions.getMaxTxnCommitTime());
            logger.info("finished: step forward in time from:"+ lastIndexTime +" to:" + toCommitTime + " max:"+ transactions.getMaxTxnCommitTime());

            if(transactions.getTransactions().size() == 0) {
                logger.info("no new transactions found lastTransactionId:" + lastTransactionId + " lastIndexTime:" + lastIndexTime);
                return;
            }
        }

        Transaction first = transactions.getTransactions().get(0);
        Transaction last = transactions.getTransactions().get(transactions.getTransactions().size() -1);
        logger.info("first:" + first.getId() +" " + new Date(first.getCommitTimeMs()) +" last:"+last.getId() + " " + new Date(last.getCommitTimeMs()));


        List<Long> transactionIds = new ArrayList<>();
        for(Transaction t : transactions.getTransactions()){
            transactionIds.add(t.getId());
        }

        List<Long> dbnodeIds = new ArrayList<>();
        for(Node node : client.getNodes(transactionIds)){
            //logger.info("n:"+node);
            dbnodeIds.add(node.getId());
        }
       // logger.info("found nodes:"+dbnodeIds.size() +" " + Arrays.toString(dbnodeIds.toArray()));

        GetNodeMetadataParam p = new GetNodeMetadataParam();
        p.setNodeIds(dbnodeIds);

        try {
            List<NodeToIndex> nodesToIndex = client.getNodesToIndex(p);
            /*for(NodeToIndex nmd : nodesToIndex){
            logger.info(nmd.getNodeMetadata().getType() + " " + nmd.getNodeMetadata().getNodeRef() +" Reader:" + Arrays.toString(nmd.getReader().getReaders().toArray()));
            }*/
        }catch(javax.ws.rs.ProcessingException e){
            logger.error("error unmarschelling nodes: " + Arrays.toString(p.getNodeIds().toArray()),e);
        }



        if(lastIndexTime < 1) {
            this.lastIndexTime = last.getCommitTimeMs();
        }else{
            this.lastIndexTime = toCommitTime;
        }
        this.lastTransactionId = last.getId();
        logger.info("finished lastTransactionId:" + this.lastTransactionId +" transactions:" + Arrays.toString(transactionIds.toArray()) +" size:" + transactions.getTransactions().size() + " dbnodeIds:"+dbnodeIds.size());
    }
}
