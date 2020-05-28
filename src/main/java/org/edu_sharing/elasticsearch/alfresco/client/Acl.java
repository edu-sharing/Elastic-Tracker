package org.edu_sharing.elasticsearch.alfresco.client;

public class Acl {

    long id;
    long aclChangeSetId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAclChangeSetId() {
        return aclChangeSetId;
    }

    public void setAclChangeSetId(long aclChangeSetId) {
        this.aclChangeSetId = aclChangeSetId;
    }
}
