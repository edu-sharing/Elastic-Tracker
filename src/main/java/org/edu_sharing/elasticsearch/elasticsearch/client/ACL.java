package org.edu_sharing.elasticsearch.elasticsearch.client;

public class ACL {

    int aclId;
    long aclCommitTime;

    public int getAclId() {
        return aclId;
    }

    public void setAclId(int aclId) {
        this.aclId = aclId;
    }

    public long getAclCommitTime() {
        return aclCommitTime;
    }

    public void setAclCommitTime(long aclCommitTime) {
        this.aclCommitTime = aclCommitTime;
    }
}
