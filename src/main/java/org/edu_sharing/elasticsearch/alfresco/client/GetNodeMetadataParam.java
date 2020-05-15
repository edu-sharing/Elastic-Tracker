package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.List;

public class GetNodeMetadataParam {
    List<Long> nodeIds = new ArrayList<Long>();


    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds) {
        this.nodeIds = nodeIds;
    }
}
