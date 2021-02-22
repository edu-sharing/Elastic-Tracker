package org.edu_sharing.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
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

    @Autowired
    StatisticsTracker statisticsTracker;

    Logger logger = LogManager.getLogger(TrackerJob.class);

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track(){
        boolean aclChanges=aclTracker.track();
        boolean transactionChanges=transactionTracker.track();
        if(aclChanges || transactionChanges){
            logger.info("recursiv aclChanges:" + aclChanges +" transactionChanges:"+transactionChanges);
            track();
        }
    }
    @Scheduled(fixedDelayString = "${statistic.delay}")
    public void trackStatistics(){
        statisticsTracker.track();
    }
}
