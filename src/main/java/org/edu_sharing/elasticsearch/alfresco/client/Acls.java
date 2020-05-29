package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.List;

public class Acls {
    List<Acl> acls = new ArrayList<>();

    public List<Acl> getAcls() {
        return acls;
    }

    public void setAcls(List<Acl> acls) {
        this.acls = acls;
    }
}
