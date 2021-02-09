package org.edu_sharing.elasticsearch.elasticsearch.client;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
public class ElasticsearchConfig {

    @Value("${elastic.host}")
    String elasticHost;

    @Value("${elastic.port}")
    int elasticPort;

    @Value("${elastic.protocol}")
    String elasticProtocol;

    @Value("${elastic.socketTimeout}")
    int elasticSocketTimeout;

    @Value("${elastic.connectTimeout}")
    int elasticConnectTimeout;

    @Value("${elastic.connectionRequestTimeout}")
    int elasticConnectionRequestTimeout;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient client() {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(elasticHost, elasticPort, elasticProtocol)).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(elasticConnectTimeout)
                                .setSocketTimeout(elasticSocketTimeout)
                                .setConnectionRequestTimeout(elasticConnectionRequestTimeout)));
    }
}
