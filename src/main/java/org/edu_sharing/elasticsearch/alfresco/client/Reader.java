package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.List;

public class Reader {
    long aclId;
    long aclChangeSetId;
    String tenantDomain;
    List<String> readers;
    List<String> denied;

    public long getAclId() {
        return aclId;
    }

    public void setAclId(long aclId) {
        this.aclId = aclId;
    }

    public long getAclChangeSetId() {
        return aclChangeSetId;
    }

    public void setAclChangeSetId(long aclChangeSetId) {
        this.aclChangeSetId = aclChangeSetId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public List<String> getReaders() {
        return readers;
    }

    public void setReaders(List<String> readers) {
        this.readers = readers;
    }

    public List<String> getDenied() {
        return denied;
    }

    public void setDenied(List<String> denied) {
        this.denied = denied;
    }
}
