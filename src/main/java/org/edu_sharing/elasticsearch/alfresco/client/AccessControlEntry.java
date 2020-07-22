package org.edu_sharing.elasticsearch.alfresco.client;

public class AccessControlEntry {

    String permission;
    String authority;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
