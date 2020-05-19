package org.edu_sharing.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.Tx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
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

    @Value("${allowed.types}")
    String allowedTypes;

    long lastFromCommitTime = -1;
    long lastTransactionId = -1;

    final static int maxResults = 50;

    //in ms
    final static int nextTimeStep = 36000000;

    final static String storeWorkspace = "workspace://SpacesStore";

    Logger logger = LogManager.getLogger(Tracker.class);

    @PostConstruct
    public void init()  throws IOException{
        Tx txn = null;
        try {
            txn = elasticClient.getTransaction();
            if(txn != null){
                lastFromCommitTime = txn.getTxnCommitTime();
                logger.info("got last transaction from index txnCommitTime:" + txn.getTxnCommitTime() +" txnId" +txn.getTxnId());
            }
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            throw e;
        }
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void track(){
        logger.info("starting lastTransactionId:" +lastTransactionId+ " lastFromCommitTime:" + lastFromCommitTime +" " +  new Date(lastFromCommitTime));

        long toCommitTime = lastFromCommitTime + Tracker.nextTimeStep;
        Transactions transactions = (lastFromCommitTime < 1)
                ? client.getTransactions(0L,2000L,null,null, 1, Tracker.storeWorkspace)
                : client.getTransactions(null, null, lastFromCommitTime, toCommitTime, Tracker.maxResults, Tracker.storeWorkspace );


        if(transactions.getTransactions().size() == 0){

            if(toCommitTime > transactions.getMaxTxnCommitTime()){
                logger.info("index is up to date:" + lastTransactionId + " lastFromCommitTime:" + lastFromCommitTime);
                //+1 to prevent repeating the last transaction over and over
                //not longer necessary when we remember last transaction id in idx
                this.lastFromCommitTime = transactions.getMaxTxnCommitTime() + 1;
                return;
            }

            logger.info("start   : step forward in time to find next transaction from:" + lastFromCommitTime +" to:" + toCommitTime + " max:"+ transactions.getMaxTxnCommitTime());
            do{

                lastFromCommitTime += Tracker.nextTimeStep;
                toCommitTime = lastFromCommitTime + Tracker.nextTimeStep;
                transactions = client.getTransactions(null,null, lastFromCommitTime, toCommitTime, Tracker.maxResults, Tracker.storeWorkspace);
            }while(transactions.getTransactions().size() == 0 && toCommitTime < transactions.getMaxTxnCommitTime());
            logger.info("finished: step forward in time from:"+ lastFromCommitTime +" to:" + toCommitTime + " max:"+ transactions.getMaxTxnCommitTime());

            if(transactions.getTransactions().size() == 0) {
                logger.info("no new transactions found lastTransactionId:" + lastTransactionId + " lastFromCommitTime:" + lastFromCommitTime);
                return;
            }
        }

        Transaction first = transactions.getTransactions().get(0);
        Transaction last = transactions.getTransactions().get(transactions.getTransactions().size() -1);

        if(lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }else{
            this.lastFromCommitTime = toCommitTime;
        }
        this.lastTransactionId = last.getId();

        /**
         * add transactionsIds as getNodes Param
         */
        List<Long> transactionIds = new ArrayList<>();
        for(Transaction t : transactions.getTransactions()){
            transactionIds.add(t.getId());
        }

        /**
         * get nodes
         */
        List<Node> nodes =  client.getNodes(transactionIds);

        /**
         * get node metadata
         */
        try {
            List<NodeData> nodeData = client.getNodeData(nodes);

            List<NodeData> toDelete = new ArrayList<NodeData>();
            List<NodeData> toIndex = new ArrayList<NodeData>();
            for(NodeData data : nodeData){

                if(allowedTypes != null && !allowedTypes.trim().equals("")){
                    String[] allowedTypesArray = allowedTypes.split(",");
                    String type = data.getNodeMetadata().getType();
                    if(!Arrays.asList(allowedTypesArray).contains(type)){
                        logger.info("ignoring type:" + type);
                        continue;
                    }
                }

                if(data.getNode().getStatus().equals("d")){
                    toDelete.add(data);
                }else {
                    toIndex.add(data);
                }
            }
            elasticClient.delete(toDelete);
            elasticClient.index(toIndex);

            //remember for the next start of tracker
            elasticClient.setTransaction(lastFromCommitTime,transactionIds.get(transactionIds.size() - 1));
        }catch(javax.ws.rs.ProcessingException e){
            logger.error("error unmarshalling NodeMetadata: " + Arrays.toString(nodes.toArray()),e);
        }catch(IOException e){
            logger.error(e.getMessage(),e);
        }


        logger.info("finished lastTransactionId:" + this.lastTransactionId +
                " transactions:" + Arrays.toString(transactionIds.toArray()) +
                " nodes:" + nodes.size());
    }
}
