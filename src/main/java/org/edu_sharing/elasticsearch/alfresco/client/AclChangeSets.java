package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.List;

public class AclChangeSets {

    List<AclChangeSet> aclChangeSets;

    Long maxChangeSetCommitTime;
    Long maxChangeSetId;

    public List<AclChangeSet> getAclChangeSets() {
        return aclChangeSets;
    }

    public void setAclChangeSets(List<AclChangeSet> aclChangeSets) {
        this.aclChangeSets = aclChangeSets;
    }

    public Long getMaxChangeSetCommitTime() {
        return maxChangeSetCommitTime;
    }

    public void setMaxChangeSetCommitTime(Long maxChangeSetCommitTime) {
        this.maxChangeSetCommitTime = maxChangeSetCommitTime;
    }

    public Long getMaxChangeSetId() {
        return maxChangeSetId;
    }

    public void setMaxChangeSetId(Long maxChangeSetId) {
        this.maxChangeSetId = maxChangeSetId;
    }
}
