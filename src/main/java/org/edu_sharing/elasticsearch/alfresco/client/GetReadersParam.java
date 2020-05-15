package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.List;

public class GetReadersParam {
    List<Long> aclIds;

    public List<Long> getAclIds() {
        return aclIds;
    }

    public void setAclIds(List<Long> aclIds) {
        this.aclIds = aclIds;
    }
}
