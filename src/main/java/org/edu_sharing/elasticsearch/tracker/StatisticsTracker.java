package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.edu_sharing.client.NodeStatistic;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.StatisticTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatisticsTracker {

    @Value("${statistic.historyInDays}")
    long historyInDays;

    @Autowired
    private ElasticsearchClient elasticClient;

    @Autowired
    private EduSharingClient eduSharingClient;




    Logger logger = LoggerFactory.getLogger(StatisticsTracker.class);


    public void track(){
        try {

            long trackTs = System.currentTimeMillis();
            long trackFromTime = trackTs - (historyInDays * 24L * 60L * 60L * 1000L);
            StatisticTimestamp statisticTimestamp = elasticClient.getStatisticTimestamp();
            if(statisticTimestamp != null && statisticTimestamp.isAllInIndex()){
                trackFromTime = statisticTimestamp.getStatisticTimestamp();
                logger.info("starting from last run " + new Date(trackFromTime));
            }else{
                logger.info("starting from history " + new Date(trackFromTime));
            }

            Map<String,List<NodeStatistic>> nodeStatistics = new HashMap<>();
            List<String> statistics = eduSharingClient.getStatisticsNodeIds(trackFromTime);
            logger.info("found "+statistics.size() + " statistic changes");
            for(String nodeId : statistics){
                List<NodeStatistic> statisticsForNode = eduSharingClient.getStatisticsForNode(nodeId, trackFromTime);
                nodeStatistics.put(nodeId,statisticsForNode);
            }
            boolean allNodesInIndex = elasticClient.updateNodeStatistics(nodeStatistics);
            elasticClient.setStatisticTimestamp(trackTs,allNodesInIndex);
            elasticClient.refresh(ElasticsearchClient.INDEX_WORKSPACE);
            elasticClient.cleanUpNodeStatistics(statistics);
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
        }
    }
}
