package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class NodeStatistic {

    String timestamp;

    Map<String,Integer> counts;

    public void setCounts(Map<String, Integer> counts) {
        this.counts = counts;
    }

    @JsonProperty("counts")
    public Map<String, Integer> getCounts() {
        return counts;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }
}
