package org.edu_sharing.elasticsearch.elasticsearch.client;

public class StatisticTimestamp {
    long statisticTimestamp;
    boolean allInIndex;

    public long getStatisticTimestamp() {
        return statisticTimestamp;
    }

    public void setStatisticTimestamp(long statisticTimestamp) {
        this.statisticTimestamp = statisticTimestamp;
    }

    public boolean isAllInIndex() {
        return allInIndex;
    }

    public void setAllInIndex(boolean allInIndex) {
        this.allInIndex = allInIndex;
    }
}
