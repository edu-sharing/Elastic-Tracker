package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeData {
    NodeMetadata nodeMetadata;
    Reader reader;
    Node node;
    String fullText;

    Map<String,List<String>> permissions;


    Map<String, Map<String, List<String>>> valueSpaces = new HashMap<>();

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

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Map<String, Map<String, List<String>>> getValueSpaces() {
        return valueSpaces;
    }

    public void setValueSpaces(Map<String, Map<String, List<String>>> valueSpaces) {
        this.valueSpaces = valueSpaces;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setAccessControlList(AccessControlList accessControlList) {
        permissions = new HashMap<>();
        for(AccessControlEntry ace : accessControlList.getAces()){
            List<String> authorities = permissions.get(ace.permission);
            if(authorities == null){
                authorities = new ArrayList<>();
            }
            if(!authorities.contains(ace.getAuthority())){
                authorities.add(ace.getAuthority());
            }
            permissions.put(ace.getPermission(),authorities);
        }
    }

    public Map<String,List<String>> getPermissions(){
        return permissions;
    }
}
