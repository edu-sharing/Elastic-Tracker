package org.edu_sharing.elasticsearch.alfresco.client;

import java.util.ArrayList;
import java.util.List;

public class GetNodeParam {

    public GetNodeParam(){

    }

    List<Long> txnIds = new ArrayList<Long>();


    public List<Long> getTxnIds() {
        return txnIds;
    }

    public void setTxnIds(List<Long> txnIds) {
        this.txnIds = txnIds;
    }
}
