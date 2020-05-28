package org.edu_sharing.elasticsearch.alfresco.client;

public class AclChangeSet {
    long id;
    long commitTimeMs;
    int aclCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCommitTimeMs() {
        return commitTimeMs;
    }

    public void setCommitTimeMs(long commitTimeMs) {
        this.commitTimeMs = commitTimeMs;
    }

    public int getAclCount() {
        return aclCount;
    }

    public void setAclCount(int aclCount) {
        this.aclCount = aclCount;
    }
}
