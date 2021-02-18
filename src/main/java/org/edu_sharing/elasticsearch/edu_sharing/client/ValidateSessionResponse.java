package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateSessionResponse {


    boolean isValidLogin;
    boolean isAdmin;
    String currentScope;
    String userHome;
    long sessionTimeout;
    String statusCode;
    String authorityName;
    boolean isGuest;


    public boolean isValidLogin() {
        return isValidLogin;
    }

    public void setValidLogin(boolean validLogin) {
        isValidLogin = validLogin;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getCurrentScope() {
        return currentScope;
    }

    public void setCurrentScope(String currentScope) {
        this.currentScope = currentScope;
    }

    public String getUserHome() {
        return userHome;
    }

    public void setUserHome(String userHome) {
        this.userHome = userHome;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public void setGuest(boolean guest) {
        isGuest = guest;
    }
}
