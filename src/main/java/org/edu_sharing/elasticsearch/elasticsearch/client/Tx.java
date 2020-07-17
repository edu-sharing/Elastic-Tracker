package org.edu_sharing.elasticsearch.elasticsearch.client;

public class Tx {
    long txnId;
    long txnCommitTime;

    public long getTxnId() {
        return txnId;
    }

    public void setTxnId(long txnId) {
        this.txnId = txnId;
    }

    public long getTxnCommitTime() {
        return txnCommitTime;
    }

    public void setTxnCommitTime(long txnCommitTime) {
        this.txnCommitTime = txnCommitTime;
    }
}
