package org.edu_sharing.elasticsearch.tracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.ACLChangeSet;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ACLTracker {

    @Autowired
    private AlfrescoWebscriptClient client;

    @Autowired
    private ElasticsearchClient elasticClient;

    @Autowired
    private EduSharingClient eduSharingClient;

    @Value("${allowed.types}")
    String allowedTypes;

    long lastFromCommitTime = -1;
    long lastACLChangeSetId = -1;

    final static int maxResults = 1000;

    @Value("${tracker.timestep:36000000}")
    int nextTimeStep;

    final static String storeWorkspace = "workspace://SpacesStore";

    Logger logger = LogManager.getLogger(ACLTracker.class);

    @PostConstruct
    public void init()  throws IOException{
        ACLChangeSet aclChangeSet = null;
        try {
            aclChangeSet = elasticClient.getACL();
            if(aclChangeSet != null){
                lastFromCommitTime = aclChangeSet.getAclCommitTime();
                lastACLChangeSetId = aclChangeSet.getAclId();
                logger.info("got last aclChangeSet from index aclCommitTime:" + aclChangeSet.getAclCommitTime() +" aclId" + aclChangeSet.getAclId());
            }
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            throw e;
        }
    }



    @Scheduled(cron = "*/5 * * * * *")
    public void track(){
        logger.info("starting lastACLChangeSetId:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime +" " +  new Date(lastFromCommitTime));


        AclChangeSets aclChangeSets = (lastACLChangeSetId < 1)
                ? client.getAclChangeSets(0L,2000L,1)
                : client.getAclChangeSets(lastACLChangeSetId, lastACLChangeSetId + ACLTracker.maxResults,ACLTracker.maxResults);


        //initialize
        if(lastACLChangeSetId < 1) lastACLChangeSetId = aclChangeSets.getAclChangeSets().get(0).getId();

        //step forward
        if(aclChangeSets.getMaxChangeSetId() > (lastACLChangeSetId + ACLTracker.maxResults)){
            lastACLChangeSetId += ACLTracker.maxResults;
        }else{
            lastACLChangeSetId = aclChangeSets.getMaxChangeSetId();
        }


        if(aclChangeSets.getAclChangeSets().size() == 0){

            if(aclChangeSets.getMaxChangeSetId() <= lastACLChangeSetId){
                logger.info("index is up to date:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime);
                //+1 to prevent repeating the last transaction over and over
                //not longer necessary when we remember last transaction id in idx
                this.lastFromCommitTime = aclChangeSets.getMaxChangeSetId() + 1;
                return;
            }else{

                logger.info("did not found new transactions in last transaction block min:" + (lastACLChangeSetId - ACLTracker.maxResults) +" max:"+ lastACLChangeSetId);
            }
        }

        AclChangeSet first = aclChangeSets.getAclChangeSets().get(0);
        AclChangeSet last = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() -1);

        if(lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }


        GetAclsParam param = new GetAclsParam();
        for(AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets()){
            param.getAclChangeSetIds().add(aclChangeSet.getId());
        }

        Acls acls = client.getAcls(param);
        for(Acl acl : acls.getAcls()){

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
        //filter non workspace://SpacesStore
        nodes = nodes
                .stream()
                .filter(n -> n.getNodeRef().startsWith("workspace://SpacesStore/"))
                .collect(Collectors.toList());

        if(nodes.size() == 0){
            return;
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

        /**
         * get node metadata
         *
         * use every single node to get metadata instead of bulk to prevent damaged nodes break other nodes
         */
        List<NodeData> nodeData = new ArrayList<NodeData>();
        for(Node node : nodes){
            try {
                List<NodeData> nodeDataTmp = client.getNodeData(Arrays.asList(new Node[] {node}));
                nodeData.addAll(nodeDataTmp);
            }catch(javax.ws.rs.ProcessingException e){
                logger.error("error unmarshalling NodeMetadata for node " + node,e);
            }
        }
        try{

            List<NodeData> toIndex = new ArrayList<NodeData>();
            for(NodeData data : nodeData){

                if(allowedTypes != null && !allowedTypes.trim().equals("")){
                    String[] allowedTypesArray = allowedTypes.split(",");
                    String type = data.getNodeMetadata().getType();
                    if(!Arrays.asList(allowedTypesArray).contains(type)){
                        logger.debug("ignoring type:" + type);
                        continue;
                    }
                }

                eduSharingClient.translateValuespaceProps(data);
                toIndex.add(data);

            }
            elasticClient.delete(toDelete);
            elasticClient.index(toIndex);

            //remember for the next start of tracker
            elasticClient.setTransaction(lastFromCommitTime,transactionIds.get(transactionIds.size() - 1));
            if(lastFromCommitTime > last.getCommitTimeMs()){
                logger.info("reseting lastFromCommitTime old:" +lastFromCommitTime +" new "+last.getCommitTimeMs());
                lastFromCommitTime = last.getCommitTimeMs() + 1;
            }
        }catch(IOException e){
            logger.error(e.getMessage(),e);
        }


        logger.info("finished lastACLChangeSetId:" + last.getId() +
                " transactions:" + Arrays.toString(transactionIds.toArray()) +
                " nodes:" + nodes.size());
    }
}
