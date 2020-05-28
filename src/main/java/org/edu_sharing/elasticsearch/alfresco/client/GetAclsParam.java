package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.List;

public class GetAclsParam {

    List<Long> aclChangeSetIds = new ArrayList<>();

    public List<Long> getAclChangeSetIds() {
        return aclChangeSetIds;
    }

    public void setAclChangeSetIds(List<Long> aclChangeSetIds) {
        this.aclChangeSetIds = aclChangeSetIds;
    }
}
