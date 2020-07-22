package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.List;

public class AccessControlList {
    long aclId;
    boolean inherits;

    List<AccessControlEntry> aces;

    public long getAclId() {
        return aclId;
    }

    public void setAclId(long aclId) {
        this.aclId = aclId;
    }

    public List<AccessControlEntry> getAces() {
        return aces;
    }

    public void setAces(List<AccessControlEntry> aces) {
        this.aces = aces;
    }

    public boolean isInherits() {
        return inherits;
    }

    public void setInherits(boolean inherits) {
        this.inherits = inherits;
    }
}
