package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.List;

public class ReadersACL {

    Object status;

    Object message;

    Object exception;

    Object callstack;

    Object server;

    Object time;

    List<Reader> aclsReaders;

    public List<Reader> getAclsReaders() {
        return aclsReaders;
    }

    public void setAclsReaders(List<Reader> aclsReaders) {
        this.aclsReaders = aclsReaders;
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        System.out.println("STATUS:"+status);
        this.status = status;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        System.out.println("MESS:"+message);
        this.message = message;
    }

    public Object getException() {
        return exception;
    }

    public void setException(Object exception) {
        this.exception = exception;
    }

    public Object getCallstack() {
        return callstack;
    }

    public void setCallstack(Object callstack) {
        this.callstack = callstack;
    }

    public Object getServer() {
        return server;
    }

    public void setServer(Object server) {
        this.server = server;
    }

    public Object getTime() {
        return time;
    }

    public void setTime(Object time) {
        this.time = time;
    }
}
