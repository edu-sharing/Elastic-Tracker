package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrackerJob {

    @Value("${loglevel}")
    String loglevel;

    @Autowired
    TransactionTracker transactionTracker;

    @Autowired
    ACLTracker aclTracker;

    TrackerJob() {

    }

    // every 5s - waits for previous invocation
    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void trackAcl() {
        aclTracker.track();
    }

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void trackTx() {
        transactionTracker.track();
    }
}
