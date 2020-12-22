package org.edu_sharing.elasticsearch.edu_sharing.client;

public class PreviewData {
    private String mimetype;
    private byte[] data;

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
