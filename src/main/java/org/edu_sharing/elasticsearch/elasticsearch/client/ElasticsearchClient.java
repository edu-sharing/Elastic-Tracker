package org.edu_sharing.elasticsearch.elasticsearch.client;


import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.alfresco.client.NodeMetadata;
import org.edu_sharing.elasticsearch.alfresco.client.Reader;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Component
public class ElasticsearchClient {

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

    Logger logger = LogManager.getLogger(ElasticsearchClient.class);

    public static String INDEX_WORKSPACE = "workspace";

    public static String INDEX_TRANSACTIONS = "transactions";

    public static String TYPE = "node";

    final static String ID_TRANSACTION = "1";

    final static String ID_ACL_CHANGESET = "2";

    String homeRepoId;

    @Autowired
    private EduSharingClient eduSharingClient;

    @PostConstruct
    public void init() throws IOException {
        createIndexIfNotExists(INDEX_TRANSACTIONS);
        createIndexWorkspace();
        this.homeRepoId = eduSharingClient.getHomeRepository().getId();
    }

    private void createIndexIfNotExists(String index) throws IOException{
        GetIndexRequest request = new GetIndexRequest(index);
        RestHighLevelClient client = getClient();
        if(!client.indices().exists(request,RequestOptions.DEFAULT)){
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        }
        client.close();
    }

    public void addCollection(NodeData usageNode){
        String ccmNamespacePrefix = "{http://www.campuscontent.de/model/1.0}";
        String propIONodeId = ccmNamespacePrefix + "usageparentnodeid";
        String nodeIdIO = (String)usageNode.getNodeMetadata().getProperties().get(propIONodeId);
        QueryBuilders.termQuery("noderef.id",nodeIdIO);
    }

    public void updateReader(long dbid, Reader reader) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("permissions");
            builder.field("read",reader.getReaders());
            builder.endObject();
        }
        builder.endObject();
        this.update(dbid,builder);
    }

    public void update(long dbId, XContentBuilder builder) throws IOException{
        RestHighLevelClient client = getClient();
        UpdateRequest request = new UpdateRequest(
                INDEX_WORKSPACE,
                Long.toString(dbId)).doc(builder);
        UpdateResponse updateResponse = client.update(
                request, RequestOptions.DEFAULT);
        String index = updateResponse.getIndex();
        String id = updateResponse.getId();
        long version = updateResponse.getVersion();
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.error("object did not exist");
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {

        } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {

        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {

        }
        client.close();
    }

    public void index(List<NodeData> nodes) throws IOException{
        RestHighLevelClient client = getClient();

        for(NodeData nodeData: nodes) {
            NodeMetadata node = nodeData.getNodeMetadata();
            String storeRefProtocol = Tools.getProtocol(node.getNodeRef());
            String storeRefIdentifier = Tools.getIdentifier(node.getNodeRef());
            String storeRef = Tools.getStoreRef(node.getNodeRef());
                XContentBuilder builder = jsonBuilder();
                builder.startObject();
                {
                    builder.field("aclId",  node.getAclId());
                    builder.field("txnId",node.getTxnId());
                    builder.field("dbid",node.getId());

                    String id = node.getNodeRef().split("://")[1].split("/")[1];
                    builder.startObject("nodeRef")
                            .startObject("storeRef")
                                .field("protocol",storeRefProtocol)
                                .field("identifier",storeRefIdentifier)
                            .endObject()
                            .field("id",id)
                    .endObject();

                    builder.field("owner", node.getOwner());
                    builder.field("type",node.getType());

                    //valuespaces
                    if(nodeData.getValueSpaces().size() > 0){
                        builder.startObject("i18n");
                        for(Map.Entry<String,Map<String,List<String>>> entry : nodeData.getValueSpaces().entrySet())      {
                            String language = entry.getKey().split("-")[0];
                            builder.startObject(language);
                            for(Map.Entry<String,List<String>> valuespace : entry.getValue().entrySet() ){

                                String key = Constants.getValidLocalName(valuespace.getKey());
                                if(key != null) {
                                    builder.field(key, valuespace.getValue());
                                }else{
                                    logger.error("unknown valuespace property: " + valuespace.getKey());
                                }
                            }
                            builder.endObject();
                        }
                        builder.endObject();
                    }

                    if(node.getPaths() != null && node.getPaths().size() > 0){
                        String[] pathEle = node.getPaths().get(0).getApath().split("/");
                        builder.field("path", Arrays.copyOfRange(pathEle,1,pathEle.length - 1));
                    }

                    builder.startObject("permissions");
                    builder.field("read",nodeData.getReader().getReaders());
                    builder.endObject();

                    //content
                    /**
                     *     "{http://www.alfresco.org/model/content/1.0}content": {
                     *    "contentId": "279",
                     *    "encoding": "UTF-8",
                     *    "locale": "de_DE_",
                     *    "mimetype": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                     *    "size": "8385"
                     * },
                     */
                    LinkedHashMap content = (LinkedHashMap)node.getProperties().get("{http://www.alfresco.org/model/content/1.0}content");
                    if(content != null){

                        builder.startObject("content");
                        builder.field("contentId", content.get("contentId"));
                        builder.field("encoding", content.get("encoding"));
                        builder.field("locale", content.get("locale"));
                        builder.field("mimetype", content.get("mimetype"));
                        builder.field("size", content.get("size"));
                        if(nodeData.getFullText() != null){
                            builder.field("fulltext", nodeData.getFullText());
                        }
                        builder.endObject();
                    }



                    builder.startObject("properties");
                    for(Map.Entry<String, Serializable> prop : node.getProperties().entrySet()) {

                        String key = Constants.getValidLocalName(prop.getKey());
                        if(key == null){
                            logger.error("unknown namespace: " + prop.getKey());
                            continue;
                        }

                        Serializable value = prop.getValue();

                        if(prop.getValue() instanceof List){
                            List listvalue = (List)prop.getValue();

                            //i.e. cm:title
                            if( !listvalue.isEmpty() && listvalue.get(0) instanceof Map){
                                value = getMultilangValue(listvalue);
                            }

                            //i.e. cclom:general_keyword
                            if( !listvalue.isEmpty() && listvalue.get(0) instanceof List){
                                List<String> mvValue = new ArrayList<String>();
                                for(Object l : listvalue){
                                    mvValue.add(getMultilangValue((List)l));
                                }
                                if(mvValue.size() > 0){
                                    value = (Serializable) mvValue;
                                }
                            }
                        }
                        if("cm:modified".equals(key) || "cm:created".equals(key)){

                            if(prop.getValue() != null) {
                                value = Date.from(Instant.parse((String) prop.getValue())).getTime();
                            }
                        }

                        //prevent Elasticsearch exception failed to parse field's value: '1-01-07T01:00:00.000+01:00'
                        if("ccm:replicationmodified".equals(key)){
                            if(prop.getValue() != null) {
                                value = prop.getValue().toString();
                            }
                        }

                        if(value != null) {

                            try {
                                builder.field(key, value);
                            }catch(MapperParsingException e){
                                logger.error("error parsing value field:" + key +"v"+value,e);
                            }
                        }
                    }
                    builder.endObject();

                    builder.field("aspects", node.getAspects());

                }
                builder.endObject();


                IndexRequest indexRequest = new IndexRequest(INDEX_WORKSPACE)
                        .id(Long.toString(node.getId())).source(builder);

                IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                String index = indexResponse.getIndex();
                String id = indexResponse.getId();
                if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                    logger.debug("created node in elastic:" + node);
                } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                    logger.debug("updated node in elastic:" + node);
                }
                ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    logger.debug("shardInfo.getTotal() "+shardInfo.getTotal() +"!="+ "shardInfo.getSuccessful():" +shardInfo.getSuccessful());
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

    public void refresh(String index) throws IOException{
        RefreshRequest request = new RefreshRequest(index);
        RestHighLevelClient client = getClient();
        try {
            client.indices().refresh(request, RequestOptions.DEFAULT);
        }finally{
            client.close();
        }
    }

    public void indexCollections(NodeMetadata usage) throws IOException{
        String propertyUsageAppId = "{http://www.campuscontent.de/model/1.0}usageappid";
        String propertyUsageCourseId = "{http://www.campuscontent.de/model/1.0}usagecourseid";
        String propertyUsageParentNodeId = "{http://www.campuscontent.de/model/1.0}usageparentnodeid";
       // String propertyUsageDbid = "{http://www.alfresco.org/model/system/1.0}node-dbid";

        String nodeIdCollection = (String)usage.getProperties().get(propertyUsageCourseId);
        String nodeIdIO = (String)usage.getProperties().get(propertyUsageParentNodeId);
        String usageAppId = (String)usage.getProperties().get(propertyUsageAppId);
        Long usageDbId = usage.getId();


        //check if it is an collection usage
        if(!homeRepoId.equals(usageAppId)){
            return;
        }

        QueryBuilder collectionQuery = QueryBuilders.termQuery("properties.sys:node-uuid",nodeIdCollection);
        QueryBuilder ioQuery = QueryBuilders.termQuery("properties.sys:node-uuid",nodeIdIO);

        SearchHits searchHitsCollection = this.search(INDEX_WORKSPACE,collectionQuery,0,1);
        if(searchHitsCollection == null || searchHitsCollection.getTotalHits().value == 0){
            logger.error("no collection found for: " + nodeIdCollection);
            return;
        }
        SearchHit searchHitCollection = searchHitsCollection.getHits()[0];

        SearchHits ioSearchHits = this.search(INDEX_WORKSPACE,ioQuery,0,1);
        if(ioSearchHits == null || ioSearchHits.getTotalHits().value == 0){
            logger.error("no io found for: " + nodeIdIO);
            return;
        }

        SearchHit hitIO = ioSearchHits.getHits()[0];

        Map propsIo = (Map)hitIO.getSourceAsMap().get("properties");
        Map propsCollection = (Map)searchHitCollection.getSourceAsMap().get("properties");


        logger.info("adding collection data: " + propsCollection.get("cm:name") +" "+propsCollection.get("sys:node-dbid") +" IO: "+propsIo.get("cm:name") +" "+propsIo.get("sys:node-dbid"));

        List<Map<String, Object>> collections = (List<Map<String, Object>>)ioSearchHits.getHits()[0].getSourceAsMap().get("collections");
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
           builder.startArray("collections");
                if(collections != null && collections.size() > 0){
                    for(Map<String,Object> collection : collections){
                        if(!searchHitCollection.getSourceAsMap().get("dbid").equals(collection.get("dbid"))){
                            builder.startObject();
                            for(Map.Entry<String,Object> entry : collection.entrySet()){
                                builder.field(entry.getKey(),entry.getValue());
                            }
                            builder.endObject();
                        }
                    }
                }


                builder.startObject();
                for (Map.Entry<String, Object> entry : searchHitCollection.getSourceAsMap().entrySet()) {
                    builder.field(entry.getKey(), entry.getValue());
                }
                builder.field("usagedbid",usageDbId);
                builder.endObject();


            builder.endArray();
        }
        builder.endObject();
        int dbid = Integer.parseInt(hitIO.getId());
        this.update(dbid,builder);
    }

    /**
     * checks if its a collection usage by searching for collections.usagedbid, and removes replicated collection object
     * @param nodes
     * @throws IOException
     */
    public void beforeDeleteCleanupReferences(List<Node> nodes) throws IOException{
        for(Node node : nodes){

            String collectionCheckAttribute = null;
            /**
             * try it is a usage
             */
            QueryBuilder queryUsage = QueryBuilders.termQuery("collections.usagedbid",node.getId());
            SearchHits searchHitsIO = this.search(INDEX_WORKSPACE,queryUsage,0,1);
            if(searchHitsIO.getTotalHits().value > 0){
                collectionCheckAttribute = "usagedbid";
            }

            /**
             * try it is an collection
             */
            if(collectionCheckAttribute == null) {
                QueryBuilder queryCollection = QueryBuilders.termQuery("collections.dbid", node.getId());
                searchHitsIO = this.search(INDEX_WORKSPACE, queryCollection, 0, 1);
                if (searchHitsIO.getTotalHits().value > 0) {
                    collectionCheckAttribute = "dbid";
                }
            }

            //nothing to cleanup
            if(collectionCheckAttribute == null){
                logger.info("nothing to cleanup for " + node.getId());
                continue;
            }

            logger.info("cleanup collection cause " + (collectionCheckAttribute.equals("dbid")? "collection deleted" : "usage deleted"));
            SearchHit hitIO = searchHitsIO.getHits()[0];
            List<Map<String, Object>> collections = (List<Map<String, Object>>)hitIO.getSourceAsMap().get("collections");
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startArray("collections");
                if(collections != null && collections.size() > 0){
                    for(Map<String,Object> collection : collections){
                        long nodeDbId = node.getId();
                        long collectionAttValue = Long.parseLong(collection.get(collectionCheckAttribute).toString());
                        if(nodeDbId != collectionAttValue){
                            builder.startObject();
                            for(Map.Entry<String,Object> entry : collection.entrySet()){
                                builder.field(entry.getKey(),entry.getValue());
                            }
                            builder.endObject();
                        }
                    }
                }
                builder.endArray();
            }
            builder.endObject();
            int dbid = Integer.parseInt(hitIO.getId());
            this.update(dbid,builder);

        }
    }

    private String getMultilangValue(List listvalue){
        if(listvalue.size() > 1){
            // find german value i.e for Documents/Images edu folder
            String value = null;
            String defaultValue = null;

            for(Object o : listvalue){
                Map m = (Map)o;
                //default is {key="locale",value="de"},{key="value",value="Deutsch"}
                if(m.size() > 2){
                    throw new RuntimeException("language map has only one value");
                }
                defaultValue = (String)m.get("value");
                if("de".equals(m.get("locale"))){
                    value = (String)m.get("value");
                }
            }
            if(value == null) value = defaultValue;
            return value;
        }else {
            Map multilangValue = (Map) listvalue.get(0);
            return (String) multilangValue.get("value");
        }
    }

    public void setTransaction(long txnCommitTime, long transactionId) throws IOException {

        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.field("txnId", transactionId);
            builder.field("txnCommitTime",txnCommitTime);
        }
        builder.endObject();

        setNode(INDEX_TRANSACTIONS, ID_TRANSACTION,builder);
    }

    private void setNode(String index, String id, XContentBuilder builder) throws IOException {
        RestHighLevelClient client = getClient();
        IndexRequest indexRequest = new IndexRequest(index)
                .id(id).source(builder);

        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.debug("created node in elastic:" + builder);
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            logger.debug("updated node in elastic:" + builder);
        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            logger.debug("shardInfo.getTotal() "+shardInfo.getTotal() +"!="+ "shardInfo.getSuccessful():" +shardInfo.getSuccessful());
        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure :
                    shardInfo.getFailures()) {
                String reason = failure.reason();
                logger.error(failure.nodeId() +" reason:"+reason);
            }
        }
        client.close();
    }

    private GetResponse get(String index, String id) throws IOException {
        RestHighLevelClient client = getClient();
        GetRequest getRequest = new GetRequest(index,id);
        GetResponse resp = client.get(getRequest,RequestOptions.DEFAULT);
        client.close();
        return resp;
    }

    public Tx getTransaction() throws IOException {

        GetResponse resp = this.get(INDEX_TRANSACTIONS, ID_TRANSACTION);
        Tx transaction = null;
        if(resp.isExists()) {

            transaction = new Tx();
            transaction.setTxnCommitTime((Long) resp.getSource().get("txnCommitTime"));
            transaction.setTxnId((Integer) resp.getSource().get("txnId"));
        }

        return transaction;
    }


    public void setACLChangeSet(long aclChangeSetTime, long aclChangeSetId) throws IOException {
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.field("aclChangeSetId", aclChangeSetId);
            builder.field("aclChangeSetCommitTime",aclChangeSetTime);
        }
        builder.endObject();

        setNode(INDEX_TRANSACTIONS, ID_ACL_CHANGESET,builder);
    }

    public ACLChangeSet getACLChangeSet() throws IOException {
        GetResponse resp = this.get(INDEX_TRANSACTIONS, ID_ACL_CHANGESET);

        ACLChangeSet aclChangeSet = null;
        if(resp.isExists()) {
            aclChangeSet = new ACLChangeSet();
            aclChangeSet.setAclChangeSetCommitTime(Long.parseLong(resp.getSource().get("aclChangeSetCommitTime").toString()));
            aclChangeSet.setAclChangeSetId((int)resp.getSource().get("aclChangeSetId"));
        }

        return aclChangeSet;
    }


    public void delete(List<Node> nodes) throws IOException {
        RestHighLevelClient client = getClient();

        for(Node node : nodes){

            DeleteRequest request = new DeleteRequest(
                    INDEX_WORKSPACE,
                    Long.toString(node.getId()));
            DeleteResponse deleteResponse = client.delete(
                    request, RequestOptions.DEFAULT);

            String index = deleteResponse.getIndex();
            String id = deleteResponse.getId();
            long version = deleteResponse.getVersion();
            ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
            if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                logger.debug("shardInfo.getTotal() != shardInfo.getSuccessful() index:" + index +" id:"+id + " version:"+version);
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

    /**
     * @TODO
     * @throws IOException
     */
    public void createIndexWorkspace() throws IOException {
        RestHighLevelClient client = getClient();
        try {
            GetIndexRequest request = new GetIndexRequest(INDEX_WORKSPACE);

            if(client.indices().exists(request,RequestOptions.DEFAULT)){
                return;
            }

            CreateIndexRequest indexRequest = new CreateIndexRequest(INDEX_WORKSPACE);

            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {

                builder.startObject("properties");
                {
                    builder.startObject("aclId").field("type", "long").endObject();
                    builder.startObject("txnId").field("type", "long").endObject();
                    builder.startObject("dbid").field("type", "long").endObject();
                    builder.startObject("nodeRef")
                            .startObject("properties")
                                .startObject("storeRef")
                                    .startObject("properties")
                                        .startObject("protocol").field("type","keyword").endObject()
                                        .startObject("identifier").field("type","keyword").endObject()
                                    .endObject()
                                .endObject()
                                .startObject("id").field("type","keyword").endObject()
                            .endObject()
                    .endObject();
                    builder.startObject("owner").field("type","keyword").endObject();
                    builder.startObject("type").field("type","keyword").endObject();
                    //leave out i18n cause it is dynamic
                    builder.startObject("path").field("type","keyword").endObject();
                    builder.startObject("permissions")
                            .startObject("properties")
                                .startObject("read").field("type","keyword").endObject()
                            .endObject()
                    .endObject();
                    //@TODO content
                    builder.startObject("properties")
                            .startObject("properties")
                                .startObject("ccm:original").field("type","keyword").endObject()
                                .startObject("cclom:location").field("type","keyword").endObject()
                                .startObject("sys:node-uuid").field("type","keyword").endObject()
                                .startObject("cclom:format").field("type","keyword").endObject()
                                .startObject("cm:versionLabel").field("type","keyword").endObject()
                                //the others are default
                            .endObject()
                    .endObject();
                    builder.startObject("aspects").field("type","keyword").endObject();
                    builder.startObject("collections")

                            .startObject("properties")
                                .startObject("dbid").field("type","long").endObject()
                                .startObject("usagedbid").field("type","long").endObject()
                                .startObject("aclId").field("type","long").endObject()
                            .endObject()

                    .endObject();

                }
                builder.endObject();
            }
            builder.endObject();

            indexRequest.mapping(builder);
            client.indices().create(indexRequest, RequestOptions.DEFAULT);


        }catch(Exception e) {
            logger.error(e.getMessage(),e);
            throw e;
        }finally {
            client.close();
        }
    }

    public SearchHits searchForAclId(long acl) throws IOException {
        return this.search(INDEX_WORKSPACE, QueryBuilders.termQuery("aclId", acl), 0, 10000);
    }

    public SearchHits search(String index, QueryBuilder queryBuilder, int from, int size) throws IOException {
        RestHighLevelClient client = getClient();
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        client.close();
        return searchResponse.getHits();
    }

    RestHighLevelClient getClient(){

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(elasticHost, elasticPort, elasticProtocol)
                        //,new HttpHost("localhost", 9201, "http")
                )/*.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setSocketTimeout(elasticSocketTimeout)
                        .setConnectTimeout(elasticConnectTimeout)
                        .setConnectionRequestTimeout(elasticConnectionRequestTimeout)));*/
                        /*.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback(){
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                IOReactorConfig.Builder ioReactorConfigBuilder = IOReactorConfig.copy(IOReactorConfig.DEFAULT);
                                ioReactorConfigBuilder = ioReactorConfigBuilder.setConnectTimeout(elasticConnectTimeout);
                               // ioReactorConfigBuilder = ioReactorConfigBuilder.setSoTimeout(elasticSocketTimeout);
                               // ioReactorConfigBuilder = ioReactorConfigBuilder.se
                                return httpClientBuilder.setDefaultIOReactorConfig(ioReactorConfigBuilder.build());
                            }
                        })*/
                        .setRequestConfigCallback(
                                new RestClientBuilder.RequestConfigCallback() {
                                    @Override
                                    public RequestConfig.Builder customizeRequestConfig(
                                            RequestConfig.Builder requestConfigBuilder) {
                                        return requestConfigBuilder
                                                .setConnectTimeout(elasticConnectTimeout)
                                                .setSocketTimeout(elasticSocketTimeout)
                                                .setConnectionRequestTimeout(elasticConnectionRequestTimeout);

                                    }
                                }));

        return client;
    };



}
