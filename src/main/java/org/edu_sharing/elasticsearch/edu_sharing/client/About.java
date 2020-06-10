package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class About {

    String themesUrl;
    long lastCacheUpdate;

    @JsonIgnore
    Object version;

    @JsonIgnore
    Object services;


    public String getThemesUrl() {
        return themesUrl;
    }

    public void setThemesUrl(String themesUrl) {
        this.themesUrl = themesUrl;
    }

    public long getLastCacheUpdate() {
        return lastCacheUpdate;
    }

    public void setLastCacheUpdate(long lastCacheUpdate) {
        this.lastCacheUpdate = lastCacheUpdate;
    }

    public Object getVersion() {
        return version;
    }

    public void setVersion(Object version) {
        this.version = version;
    }

    public Object getServices() {
        return services;
    }

    public void setServices(Object services) {
        this.services = services;
    }
}
