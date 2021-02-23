package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.edu_sharing.client.NodeStatistic;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.Tx;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.elasticsearch.common.util.iterable.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class TransactionTracker {

    @Autowired
    private AlfrescoWebscriptClient client;

    @Autowired
    private ElasticsearchClient elasticClient;

    @Autowired
    private EduSharingClient eduSharingClient;

    @Value("${allowed.types}")
    String allowedTypes;

    List<String> subTypes = Arrays.asList(new String[]{"ccm:io","ccm:rating","ccm:comment","ccm:usage"});

    @Value("${index.storerefs}")
    List<String> indexStoreRefs;

    @Value("${threading.threadCount}")
    Integer threadCount;

    long lastFromCommitTime = -1;
    long lastTransactionId = -1;

    final static int maxResults = 500;

    Logger logger = LoggerFactory.getLogger(TransactionTracker.class);
    private ForkJoinPool threadPool;

    @Value("${statistic.historyInDays}")
    long historyInDays;

    @PostConstruct
    public void init()  throws IOException{
        Tx txn = null;
        try {
            txn = elasticClient.getTransaction();
            if(txn != null){
                lastFromCommitTime = txn.getTxnCommitTime();
                lastTransactionId = txn.getTxnId();
                logger.info("got last transaction from index txnCommitTime:" + txn.getTxnCommitTime() +" txnId" +txn.getTxnId());
            }
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            throw e;
        }
        threadPool = new ForkJoinPool(threadCount);

    }


    public boolean track(){
        logger.info("starting lastTransactionId:" +lastTransactionId+ " lastFromCommitTime:" + lastFromCommitTime +" " +  new Date(lastFromCommitTime));

        eduSharingClient.refreshValuespaceCache();

        ///test
       /* long ts = System.currentTimeMillis() - 10000000;
        List<String> statistics = eduSharingClient.getStatisticsNodeIds(ts);
        for(String statistic : statistics){
            System.out.println("statistics nodeId: "+statistic);
            List<NodeStatistic> statisticsForNode = eduSharingClient.getStatisticsForNode(statistic, ts);
            for(NodeStatistic ns : statisticsForNode){
                System.out.println("Statistic for node:" +ns.getTimestamp());
                for(Map.Entry<String,Integer> entry : ns.getCounts().entrySet()){
                    System.out.println("     " +entry.getKey() +" "+entry.getValue());
                }
            }

        }*/

        Transactions transactions = (lastTransactionId < 1)
                ? client.getTransactions(0L,500L,null,null, 1)
                : client.getTransactions(lastTransactionId, lastTransactionId + TransactionTracker.maxResults, null, null, TransactionTracker.maxResults);

        long newLastTransactionId = lastTransactionId;
        //initialize
        if(newLastTransactionId < 1){
            newLastTransactionId = transactions.getTransactions().get(0).getId();
        }else {
            //step forward
            if (transactions.getMaxTxnId() > (newLastTransactionId + TransactionTracker.maxResults)) {
                newLastTransactionId += TransactionTracker.maxResults;
            } else {
                newLastTransactionId = transactions.getMaxTxnId();
            }
        }


        if(transactions.getTransactions().size() == 0){

            lastTransactionId = newLastTransactionId;
            if(transactions.getMaxTxnId() <= lastTransactionId){
                logger.info("index is up to date:" + lastTransactionId + " lastFromCommitTime:" + lastFromCommitTime+" transactions.getMaxTxnId():"+transactions.getMaxTxnId());
                return false;
            }else{
                logger.info("did not found new transactions in last transaction block min:" + (lastTransactionId - TransactionTracker.maxResults) +" max:"+lastTransactionId  );
                return true;
            }
        }


        try {
            Tx txn = elasticClient.getTransaction();
            //long lastProcessedTxId = transactions.getTransactions().get(size -1).getId();
            if(txn != null && (txn.getTxnId() == transactions.getMaxTxnId())){
                logger.info("nothing to do.");
                return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return false;
        }


        Transaction first = transactions.getTransactions().get(0);
        Transaction last = transactions.getTransactions().get(transactions.getTransactions().size() -1);

        if(lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }



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
        //filter stores
        nodes = nodes
                .stream()
                .filter(n -> indexStoreRefs.contains(Tools.getStoreRef(n.getNodeRef())))
                .collect(Collectors.toList());

        if(nodes.size() == 0){
            lastTransactionId = newLastTransactionId;
            return true;
        }

        /**
         * collect deletes
         */
        List<Node> toDelete = new ArrayList<Node>();
        for(Node node : nodes){
            if(node.getStatus().equals("d")) {
                toDelete.add(node);
            }
        }

        //filter deletes
        nodes = nodes
                .stream()
                .filter(n -> !n.getStatus().equals("d"))
                .collect(Collectors.toList());

        List<NodeMetadata> nodeData = new ArrayList<>();
        try {
            nodeData.addAll(client.getNodeMetadata(nodes));
        } catch(Throwable t) {
            /**
             * get node metadata
             *
             * use every single node to get metadata instead of bulk to prevent damaged nodes break other nodes
             */
            for (Node node : nodes) {
                try {
                    List<NodeMetadata> nodeDataTmp = client.getNodeMetadata(Arrays.asList(new Node[]{node}));
                    nodeData.addAll(nodeDataTmp);
                } catch (javax.ws.rs.ProcessingException e) {
                    logger.error("error unmarshalling NodeMetadata for node " + node, e);
                }
            }
        }
        logger.info("getNodeData done " +nodeData.size());
        try{

            List<NodeMetadata> toIndexUsagesMd = nodeData
                    .stream()
                    .filter(n -> "ccm:usage".equals(n.getType()))
                    .collect(Collectors.toList());

            List<NodeMetadata> toIndexMd = new ArrayList<>();
            List<Node> ioSubobjectChange = new ArrayList<>();
            for(NodeMetadata data : nodeData){

                //force reindex of parent io to get subobjects
                if(subTypes.contains(data.getType())
                        && (!data.getType().equals("ccm:io") || data.getAspects().contains("ccm:io_childobject"))
                        && Constants.STORE_REF_WORKSPACE.equals(Tools.getStoreRef(data.getNodeRef()))){

                    String[] splitted = data.getPaths().get(0).getApath().split("/");
                    String parentId = splitted[splitted.length -1];
                    Serializable value = elasticClient.getProperty(Constants.STORE_REF_WORKSPACE+"/"+parentId,"dbid");
                    if(value != null){
                        Long parentDbid = ((Number)value).longValue();
                        logger.info("FOUND PARENT IO WITH "+ parentDbid);
                        //check if exists in list
                        if(!nodeData.stream().anyMatch(n -> n.getId() == parentDbid)){
                            Node n = new Node();
                            n.setId(parentDbid);
                            ioSubobjectChange.add(n);
                        }
                    }//else io does not exist in index
                }

                if(allowedTypes != null && !allowedTypes.trim().equals("")){
                    String[] allowedTypesArray = allowedTypes.split(",");
                    String type = data.getType();

                    if(!Arrays.asList(allowedTypesArray).contains(type)){
                        logger.debug("ignoring type:" + type);
                        continue;
                    }
                }
                toIndexMd.add(data);
            }

            if(ioSubobjectChange.size() > 0){
                toIndexMd.addAll(client.getNodeMetadata(ioSubobjectChange));
            }

            List<NodeData> toIndex = client.getNodeData(toIndexMd);
            for(NodeData data: toIndex) {
                threadPool.execute(() -> {
                    eduSharingClient.addPreview(data);
                    eduSharingClient.translateValuespaceProps(data);
                });
            }
            if(!threadPool.awaitQuiescence(10, TimeUnit.MINUTES)){
                logger.error("Fatal error while processing nodes: timeout of preview and transform processing");
                logger.error(nodeData.stream().map(NodeMetadata::getNodeRef).collect(Collectors.joining(", ")));
            }

            logger.info("final usable: " + toIndexUsagesMd.size() + " " + toIndex.size());

            elasticClient.beforeDeleteCleanupCollectionReplicas(toDelete);
            elasticClient.delete(toDelete);
            elasticClient.index(toIndex);

            for(NodeData nodeDataStat : toIndex){
                if(!"ccm:io".equals(nodeDataStat.getNodeMetadata().getType())
                        || !Tools.getProtocol(nodeDataStat.getNodeMetadata().getNodeRef()).equals("workspace")){
                    continue;
                }
                long trackTs = System.currentTimeMillis();
                long trackFromTime = trackTs - (historyInDays * 24L * 60L * 60L * 1000L);
                String nodeId = Tools.getUUID(nodeDataStat.getNodeMetadata().getNodeRef());
                List<NodeStatistic> statisticsForNode = eduSharingClient.getStatisticsForNode(nodeId, trackFromTime);
                Map<String,List<NodeStatistic>> updateNodeStatistics = new HashMap<>();
                updateNodeStatistics.put(nodeId,statisticsForNode);
                elasticClient.updateNodeStatistics(updateNodeStatistics);
            }

            /**
             * refresh index so that collections will be found by cacheCollections process
             */
            elasticClient.refresh(ElasticsearchClient.INDEX_WORKSPACE);
            for(NodeMetadata usage : toIndexUsagesMd) elasticClient.indexCollections(usage);

            //remember for the next start of tracker
            elasticClient.setTransaction(lastFromCommitTime,transactionIds.get(transactionIds.size() - 1));
            //set on success
            lastTransactionId = newLastTransactionId;

            if(lastFromCommitTime > last.getCommitTimeMs()){
                logger.info("reseting lastFromCommitTime old:" +lastFromCommitTime +" new "+last.getCommitTimeMs());
                lastFromCommitTime = last.getCommitTimeMs() + 1;
            }
        }catch(IOException e){
            logger.error(e.getMessage(),e);
        }


        logger.info("finished lastTransactionId:" + last.getId() +
                " transactions:" + Arrays.toString(transactionIds.toArray()) +
                " nodes:" + nodes.size());

       /* if(transactions.getMaxTxnId() <= lastTransactionId){
            return false;
        }else {
            return true;
        }*/
        return true;

    }
}
