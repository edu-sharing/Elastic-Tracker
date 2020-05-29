package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrackerJob {

    @Autowired
    TransactionTracker transactionTracker;

    @Autowired
    ACLTracker aclTracker;

    @Scheduled(cron = "*/5 * * * * *")
    public void track(){
        aclTracker.track();
        transactionTracker.track();
    }
}
