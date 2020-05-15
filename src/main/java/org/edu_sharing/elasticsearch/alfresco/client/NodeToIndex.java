package org.edu_sharing.elasticsearch.alfresco.client;

public class NodeToIndex {
    NodeMetadata nodeMetadata;
    Reader reader;

    public NodeMetadata getNodeMetadata() {
        return nodeMetadata;
    }

    public void setNodeMetadata(NodeMetadata nodeMetadata) {
        this.nodeMetadata = nodeMetadata;
    }

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }
}
