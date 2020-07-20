package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValuespaceEntries {

    String error;

    String message;

    String logLevel;

    List<ValuespaceEntry> values;

    public List<ValuespaceEntry> getValues() {
        return values;
    }

    public void setValues(List<ValuespaceEntry> values) {
        this.values = values;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}
