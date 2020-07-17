package org.edu_sharing.elasticsearch.elasticsearch.client;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public abstract class SearchHitsRunner {

    protected ElasticsearchClient elasticClient;
    public SearchHitsRunner(ElasticsearchClient elasticClient){
        this.elasticClient = elasticClient;
    }

    public void run(QueryBuilder queryBuilder)throws IOException {

        int page = 0;
        int pageSize = 5;
        SearchHits searchHits = null;
        do{
            if(searchHits != null){
                page+=pageSize;
            }
            searchHits = elasticClient.search(ElasticsearchClient.INDEX_WORKSPACE, queryBuilder, page, pageSize);
            for(SearchHit searchHit : searchHits.getHits()){
                execute(searchHit);
            }

        }while(searchHits.getTotalHits().value > page);


    }

    public abstract void execute(SearchHit hit) throws IOException;
}
