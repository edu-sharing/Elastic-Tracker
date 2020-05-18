package org.edu_sharing.elasticsearch.elasticsearch.client;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.Constants;
import org.edu_sharing.elasticsearch.alfresco.client.NodeMetadata;
import org.edu_sharing.elasticsearch.alfresco.client.NodeMetadatas;
import org.elasticsearch.action.DocWriteResponse;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Component
public class ElasticsearchClient {

    @Value("${elastic.host}")
    String elasticHost;

    @Value("${elastic.port}")
    int elasticPort;

    @Value("${elastic.protocol}")
    String elasticProtocol;

    Logger logger = LogManager.getLogger(ElasticsearchClient.class);

    public static String INDEX_WORKSPACE = "workspace";

    public static String TYPE = "node";

    public void index(List<NodeMetadata> nodes) throws IOException{
        RestHighLevelClient client = getClient();

        for(NodeMetadata node: nodes) {
                XContentBuilder builder = jsonBuilder();
                builder.startObject();
                {
                    builder.field("aclId",  node.getAclId());
                    builder.field("txnId",node.getTxnId());
                    builder.field("dbid",node.getId());
                    builder.field("nodeRef", node.getNodeRef());
                    builder.field("owner", node.getOwner());
                    builder.field("type",node.getType());

                    for(Map.Entry<String, Serializable> prop : node.getProperties().entrySet()) {

                        String key = Constants.getValidLocalName(prop.getKey());
                        if(key == null){
                            logger.error("unknown namespace: " + prop.getKey());
                            continue;
                        }
                        if(prop.getValue() instanceof List){
                            builder.array(key, prop.getValue());
                        }else{
                            builder.field(key, prop.getValue());
                        }
                    }

                }
                builder.endObject();


                IndexRequest indexRequest = new IndexRequest(INDEX_WORKSPACE)
                        .id(Long.toString(node.getId())).source(builder);

                IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                String index = indexResponse.getIndex();
                String id = indexResponse.getId();
                if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                    logger.info("created node in elastic:" + node);
                } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                    logger.info("updated node in elastic:" + node);
                }
                ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    logger.error("shardInfo.getTotal() "+shardInfo.getTotal() +"!="+ "shardInfo.getSuccessful():" +shardInfo.getSuccessful());
                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure :
                            shardInfo.getFailures()) {
                        String reason = failure.reason();
                        logger.error(failure.nodeId() +" reason:"+reason);
                    }
                }


        }

        client.close();

    }



    public void delete(List<NodeMetadata> nodeMetadatas) throws IOException {
        RestHighLevelClient client = getClient();

        for(NodeMetadata nmd : nodeMetadatas){
            DeleteRequest request = new DeleteRequest(
                    INDEX_WORKSPACE,
                    Long.toString(nmd.getId()));
            DeleteResponse deleteResponse = client.delete(
                    request, RequestOptions.DEFAULT);

            String index = deleteResponse.getIndex();
            String id = deleteResponse.getId();
            long version = deleteResponse.getVersion();
            ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
            if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                logger.error("shardInfo.getTotal() != shardInfo.getSuccessful() index:" + index +" id:"+id + " version:"+version);
            }
            if (shardInfo.getFailed() > 0) {
                for (ReplicationResponse.ShardInfo.Failure failure :
                        shardInfo.getFailures()) {
                    String reason = failure.reason();
                    logger.error(reason + "index:" + index +" id:"+id +" version:"+version);
                }
            }
        }

        client.close();
    }

    public void createIndex() throws IOException {
        try {
            RestHighLevelClient client = getClient();


            CreateIndexRequest indexRequest = new CreateIndexRequest(INDEX_WORKSPACE);

            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {

                builder.startObject("properties");
                {
                    builder.startObject("aclId").field("type", "long").endObject();
                    builder.startObject("txnId").field("type", "long").endObject();
                    builder.startObject("dbid").field("type", "long").endObject();
                    builder.startObject("nodeRef").field("type", "text").endObject();
                    builder.startObject("owner").field("type", "text").endObject();
                    builder.startObject("type").field("type", "text").endObject();
                    //builder.startObject("name").field("type", "text").endObject();
                    //builder.startObject("keywords").field("type", "keyword").endObject();
                }
                builder.endObject();
            }
            builder.endObject();

            indexRequest.mapping(builder);



        }catch(Exception e) {
            // index already exists
            // throw new RuntimeException("Elastic search init failed",e);
        }
    }

    RestHighLevelClient getClient(){
      return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elasticHost, elasticPort, elasticProtocol)
                        //,new HttpHost("localhost", 9201, "http")
                ));
    };
}
