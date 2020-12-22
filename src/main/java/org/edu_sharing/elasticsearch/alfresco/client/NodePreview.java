package org.edu_sharing.elasticsearch.alfresco.client;

public class NodePreview {
    private String mimetype;
    private byte[] small;
    private byte[] large;

    public byte[] getSmall() {
        return small;
    }

    public void setSmall(byte[] small) {
        this.small = small;
    }

    public byte[] getLarge() {
        return large;
    }

    public void setLarge(byte[] large) {
        this.large = large;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getMimetype() {
        return mimetype;
    }
}
