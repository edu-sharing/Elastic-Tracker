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
import java.util.concurrent.CompletableFuture;
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

    final static int maxResults = 500;

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

    public boolean track() {
        logger.info("starting lastACLChangeSetId:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime + " " + new Date(lastFromCommitTime));


        AclChangeSets aclChangeSets = (lastACLChangeSetId < 1)
                ? client.getAclChangeSets(0L, 500L, 1)
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
            } else {
                logger.info("did not found new aclchangesets in last aclchangeset block from:" + (lastACLChangeSetId - ACLTracker.maxResults) + " to:" + lastACLChangeSetId + " MaxChangeSetId:" +aclChangeSets.getMaxChangeSetId());
            }
            return false;
        }

        AclChangeSet first = aclChangeSets.getAclChangeSets().get(0);
        AclChangeSet last = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1);

        try {
            ACLChangeSet aclChangeSet = elasticClient.getACLChangeSet();
            if(aclChangeSet != null && (aclChangeSet.getAclChangeSetId() == aclChangeSets.getMaxChangeSetId())){
                logger.info("nothing to do.");
                return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return false;
        }


        if (lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }


        GetAclsParam param = new GetAclsParam();
        for (AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets()) {
            param.getAclChangeSetIds().add(aclChangeSet.getId());
        }


        Acls acls = client.getAcls(param);

        GetPermissionsParam grp = new GetPermissionsParam();
        Map<Long, Acl> aclIdMap = acls.getAcls().stream()
                .collect(Collectors.toMap(Acl::getId, accessControlList -> accessControlList));

        grp.setAclIds(new ArrayList<>(aclIdMap.keySet()));
        ReadersACL readers = client.getReader(grp);
        Map<Long, Reader> readersMap = readers.getAclsReaders().stream()
                .collect(Collectors.toMap(Reader::getAclId, readersList -> readersList));

        logger.debug(grp.getAclIds().toString());
        AccessControlLists accessControlLists = client.getAccessControlLists(grp);
        Map<Long, AccessControlList> accessControlListMap = accessControlLists.getAccessControlLists().stream()
                .collect(Collectors.toMap(AccessControlList::getAclId, accessControlList -> accessControlList));

        for (Acl acl : acls.getAcls()) {

            Reader reader = readersMap.get(acl.getId());
            if (reader.getAclId() != acl.getId()) {
                logger.error("reader aclid:" + reader.getAclId() + " does not match " + acl.getId());
                continue;
            }

            List<String> alfReader = reader.getReaders();
            Collections.sort(alfReader);
            /**
             *  alfresco permissions
             */
            Map<String, List<String>> permissionsAlf = new HashMap<>();
            for (AccessControlEntry ace : accessControlListMap.get(acl.getId()).getAces()) {
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
            if (!alfReader.isEmpty()) {
                permissionsAlf.put("read", alfReader);
            }
            //sort alf map keys:
            permissionsAlf = new TreeMap<>(permissionsAlf);
            Map<String, List<String>> finalPermissionsAlf = permissionsAlf;
            CompletableFuture.runAsync(() -> elasticClient.updateNodesWithAcl(acl.getId(), finalPermissionsAlf));
        }
        long lastAclChangesetid = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId();
        CompletableFuture.runAsync(() -> elasticClient.setACLChangeSet(lastFromCommitTime, lastAclChangesetid));


        logger.info("finished lastACLChangeSetId:" + last.getId());
        return true;
    }
}
