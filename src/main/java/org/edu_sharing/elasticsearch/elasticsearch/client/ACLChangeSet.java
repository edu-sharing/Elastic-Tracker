package org.edu_sharing.elasticsearch.elasticsearch.client;

public class ACLChangeSet {

    int aclChangeSetId;
    long aclChangeSetCommitTime;

    public int getAclChangeSetId() {
        return aclChangeSetId;
    }

    public void setAclChangeSetId(int aclChangeSetId) {
        this.aclChangeSetId = aclChangeSetId;
    }

    public long getAclChangeSetCommitTime() {
        return aclChangeSetCommitTime;
    }

    public void setAclChangeSetCommitTime(long aclChangeSetCommitTime) {
        this.aclChangeSetCommitTime = aclChangeSetCommitTime;
    }
}
