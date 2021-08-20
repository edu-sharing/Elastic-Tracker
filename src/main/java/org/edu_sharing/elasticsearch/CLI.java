package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(helpCommand = false, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Callable<Integer>
{
    @Autowired
    ElasticsearchClient elasticsearchClient;
    @CommandLine.Option(names = "--drop-index", description = "CAREFUL! Drops the whole current index and forces a full re-index (this can take several hours)")
    Boolean clearIndex;
    @Override
    public Integer call() throws Exception {
        if(Boolean.TRUE.equals(clearIndex)) {
            elasticsearchClient.deleteIndex(ElasticsearchClient.INDEX_TRANSACTIONS);
            System.out.println("Droped index " + ElasticsearchClient.INDEX_TRANSACTIONS);
            elasticsearchClient.deleteIndex(ElasticsearchClient.INDEX_WORKSPACE);
            System.out.println("Droped index " + ElasticsearchClient.INDEX_WORKSPACE);
            return 0;
        }
        // nothing to do, we use the mixinStandardHelpOptions
        return -1;
    }

}
