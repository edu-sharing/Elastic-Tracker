package org.edu_sharing.elasticsearch.tracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.ACLChangeSet;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

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
    public void init() throws IOException {
        ACLChangeSet aclChangeSet = null;
        try {
            aclChangeSet = elasticClient.getACLChangeSet();
            if (aclChangeSet != null) {
                lastFromCommitTime = aclChangeSet.getAclChangeSetCommitTime();
                lastACLChangeSetId = aclChangeSet.getAclChangeSetId();
                logger.info("got last aclChangeSet from index aclCommitTime:" + aclChangeSet.getAclChangeSetCommitTime() + " aclId" + aclChangeSet.getAclChangeSetId());
            }
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            throw e;
        }
    }

    public void track() {
        logger.info("starting lastACLChangeSetId:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime + " " + new Date(lastFromCommitTime));


        AclChangeSets aclChangeSets = (lastACLChangeSetId < 1)
                ? client.getAclChangeSets(0L, 2000L, 1)
                : client.getAclChangeSets(lastACLChangeSetId, lastACLChangeSetId + ACLTracker.maxResults, ACLTracker.maxResults);


        //initialize
        if (lastACLChangeSetId < 1) lastACLChangeSetId = aclChangeSets.getAclChangeSets().get(0).getId();

        //step forward
        if (aclChangeSets.getMaxChangeSetId() > (lastACLChangeSetId + ACLTracker.maxResults)) {
            lastACLChangeSetId += ACLTracker.maxResults;
        } else {
            lastACLChangeSetId = aclChangeSets.getMaxChangeSetId();
        }


        if (aclChangeSets.getAclChangeSets().size() == 0) {

            if (aclChangeSets.getMaxChangeSetId() <= lastACLChangeSetId) {
                logger.info("index is up to date:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime);
                //+1 to prevent repeating the last transaction over and over
                //not longer necessary when we remember last transaction id in idx
                this.lastFromCommitTime = aclChangeSets.getMaxChangeSetId() + 1;
                return;
            } else {

                logger.info("did not found new aclchangesets in last aclchangeset block from:" + (lastACLChangeSetId - ACLTracker.maxResults) + " to:" + lastACLChangeSetId + " MaxChangeSetId:" +aclChangeSets.getMaxChangeSetId());
                return;
            }
        }

        AclChangeSet first = aclChangeSets.getAclChangeSets().get(0);
        AclChangeSet last = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1);

        if (lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }


        GetAclsParam param = new GetAclsParam();
        for (AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets()) {
            param.getAclChangeSetIds().add(aclChangeSet.getId());
        }

        try {
            Acls acls = client.getAcls(param);

            for (Acl acl : acls.getAcls()) {
                SearchHits searchHits = elasticClient.searchForAclId(acl.getId());

                if(searchHits.getHits() == null || searchHits.getHits().length == 0){
                    logger.info("no nodes found in index for aclid:" +acl.getId());
                    continue;
                }


                GetPermissionsParam grp = new GetPermissionsParam();
                grp.setAclIds(Arrays.asList(new Long[]{acl.getId()}));
                ReadersACL readers = client.getReader(grp);
                AccessControlLists accessControlLists = client.getAccessControlLists(grp);

                Reader reader = readers.getAclsReaders().get(0);
                if(reader.getAclId() != acl.getId()){
                    logger.error("reader aclid:" + reader.getAclId() +" does not match " +acl.getId());
                    continue;
                }

                for (SearchHit hit : searchHits.getHits()) {

                    int dbid = (int)hit.getSourceAsMap().get("dbid");

                    //List<String> elasticReader = ( List<String> )hit.getSourceAsMap().get("permissions.read");
                    HashMap elasticPermissions = (HashMap)hit.getSourceAsMap().get("permissions");
                    List<String> alfReader = reader.getReaders();

                    Collections.sort(alfReader);

                    /**
                     *  alfresco permissions
                     */
                    Map<String,List<String>> permissionsAlf = new HashMap<>();
                    for(AccessControlEntry ace : accessControlLists.getAccessControlLists().get(0).getAces()) {
                        List<String> authorities = permissionsAlf.get(ace.getPermission());
                        if (authorities == null) {
                            authorities = new ArrayList<>();
                        }
                        if (!authorities.contains(ace.getAuthority())) {
                            authorities.add(ace.getAuthority());
                        }
                        Collections.sort(authorities);
                        permissionsAlf.put(ace.getPermission(), authorities);
                    }
                    permissionsAlf.put("read",alfReader);
                    //sort alf map keys:
                    permissionsAlf = new TreeMap<>(permissionsAlf);

                    /**
                     * elastic permissions
                     */
                    Map<String,List<String>> permissionsElastic = new HashMap<>();
                    for(Object key : elasticPermissions.keySet()){
                        List<String> value = (List<String>)elasticPermissions.get(key);
                        Collections.sort(value);
                        permissionsElastic.put((String)key, value);
                    }
                    //sort elastic map keys:
                    permissionsElastic = new TreeMap<>(permissionsElastic);

                    if(!permissionsAlf.equals(permissionsElastic)) {
                        elasticClient.updatePermissions(dbid, permissionsAlf);
                        logger.info("permissions updated for dbid:" + dbid);
                    }else{
                        logger.debug("permissions did not change in elastic dbid:" +dbid);
                    }
                }
            }
            long lastAclChangesetid = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId();
            elasticClient.setACLChangeSet(lastFromCommitTime, lastAclChangesetid);
        } catch (IOException e) {
            logger.error("elastic search server not reachable", e);
        }


        logger.info("finished lastACLChangeSetId:" + last.getId());
    }
}
