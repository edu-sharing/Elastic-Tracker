package org.edu_sharing.elasticsearch.elasticsearch.client;

public class Tx {
    int txnId;
    long txnCommitTime;

    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public long getTxnCommitTime() {
        return txnCommitTime;
    }

    public void setTxnCommitTime(long txnCommitTime) {
        this.txnCommitTime = txnCommitTime;
    }
}
