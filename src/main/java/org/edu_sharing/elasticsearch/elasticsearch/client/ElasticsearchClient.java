package org.edu_sharing.elasticsearch.elasticsearch.client;


import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
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
    private int nodeCounter;
    private long lastNodeCount = System.currentTimeMillis();

    @Autowired
    private AlfrescoWebscriptClient alfrescoClient;

    @PostConstruct
    public void init() throws IOException {
        createIndexIfNotExists(INDEX_TRANSACTIONS);
        createIndexWorkspace();
        this.homeRepoId = eduSharingClient.getHomeRepository().getId();
    }
    private void deleteIndex(String index) throws IOException{
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        RestHighLevelClient client = getClient();
        client.indices().delete(request, RequestOptions.DEFAULT);
        client.close();
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

    public void updatePermissions(long dbid, Map<String,List<String>> permissions) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("permissions");
            for(Map.Entry<String,List<String>> entry: permissions.entrySet()){
                builder.field(entry.getKey(),entry.getValue());
            }
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

    public void updateBulk(List<UpdateRequest> updateRequests) throws IOException{
        RestHighLevelClient client = getClient();
        BulkRequest bulkRequest = new BulkRequest(INDEX_WORKSPACE);
        for(UpdateRequest updateRequest : updateRequests){
            bulkRequest.add(updateRequest);
        }
        if(bulkRequest.numberOfActions() > 0){
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            for(BulkItemResponse item : response.getItems()){
                if(item.getFailure() != null){
                    logger.error(item.getFailureMessage());
                }
            }
        }
        client.close();
    }

    public void index(List<NodeData> nodes) throws IOException{
        logger.info("starting");
        RestHighLevelClient client = getClient();

        BulkRequest bulkRequest = new BulkRequest(INDEX_WORKSPACE);
        boolean useBulkUpdate = true;

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
                    for(Map.Entry<String,List<String>> entry : nodeData.getPermissions().entrySet()){
                        builder.field(entry.getKey(),entry.getValue());
                    }
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
                                    String mlv = getMultilangValue((List)l);
                                    if(mlv != null) {
                                        mvValue.add(mlv);
                                    }
                                }
                                if(mvValue.size() > 0){
                                    value = (Serializable) mvValue;
                                }//fix: mapper_parsing_exception Preview of field's value: '{locale=de_}']] (empty keyword)
                                else{
                                    logger.info("fallback to \\”\\” for prop "+key +" v:" + value);
                                    value = "";
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

                        //elastic maps this on date field, it gets a  failed to parse field exception when it's empty
                        if("ccm:replicationsourcetimestamp".equals(key)){
                            if(value != null && value.toString().trim().equals("")){
                                value = null;
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

                if(useBulkUpdate){
                    bulkRequest.add(indexRequest);
                }else{
                    IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                    String index = indexResponse.getIndex();
                    String id = indexResponse.getId();
                    if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                        logger.debug("created node in elastic:" + node);
                    } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                        logger.debug("updated node in elastic:" + node);
                        if(node.getType().equals("ccm:map") && node.getAspects().contains("ccm:collection")){
                            this.refresh(INDEX_WORKSPACE);
                            onUpdateRefreshCollectionReplicas(nodeData.getNodeMetadata());
                        }
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
                if(nodeCounter++%100 == 0){
                    logger.info("Processed " + nodeCounter +" nodes (" + (System.currentTimeMillis() - lastNodeCount) + "ms per last 100 nodes)");
                    lastNodeCount = System.currentTimeMillis();
                }

        }

        if(useBulkUpdate && bulkRequest.numberOfActions() > 0) {
            logger.info("starting bulk update:");
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            logger.info("finished bulk update:");

            Map<Long,NodeData> collectionNodes = new HashMap<>();
            for(NodeData nodeData: nodes) {
                NodeMetadata node = nodeData.getNodeMetadata();
                if((node.getType().equals("ccm:map") && node.getAspects().contains("ccm:collection"))
                        || (node.getType().equals("ccm:io") && !node.getAspects().contains("ccm:collection_io_reference"))){
                    collectionNodes.put(node.getId(),nodeData);
                }
            }
            logger.info("start refresh index");
            this.refresh(INDEX_WORKSPACE);
            try {
                logger.info("start RefreshCollectionReplicas");
                for (BulkItemResponse item : bulkResponse.getItems()) {
                    if (item.isFailed()) {

                        if(item.getResponse() != null){
                            logger.error("Failed indexing of " + item.getResponse().getId());
                        }else{
                            BulkItemResponse bir = (BulkItemResponse)item;
                            if(bir.getFailure() != null){
                                logger.error(bir.getFailure().getCause());
                            }
                            logger.error("Failed indexing of " + bir.getFailureMessage());
                        }

                        logger.error("Failed indexing of " + ((item.getResponse() == null) ? item : item.getResponse().getId()));
                        continue;
                    }
                    Long dbId = Long.parseLong(item.getResponse().getId());
                    NodeData nodeData = collectionNodes.get(dbId);
                    if (nodeData != null) {
                        onUpdateRefreshCollectionReplicas(nodeData.getNodeMetadata());
                    }
                }
                logger.info("finished RefreshCollectionReplicas");
            }catch(Throwable e){
                logger.error(e.getMessage(),e);
                throw e;
            }

        }
        client.close();
        logger.info("returning");
    }

    public void refresh(String index) throws IOException{
        logger.debug("starting");
        RefreshRequest request = new RefreshRequest(index);
        RestHighLevelClient client = getClient();
        try {
            client.indices().refresh(request, RequestOptions.DEFAULT);
        }finally{
            client.close();
        }
        logger.debug("returning");
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
        this.refresh(INDEX_WORKSPACE);
    }

    /**
     * checks if its a collection usage by searching for collections.usagedbid, and removes replicated collection object
     * @param nodes
     * @throws IOException
     */
    public void beforeDeleteCleanupCollectionReplicas(List<Node> nodes) throws IOException{
        logger.info("starting");

        if(nodes.size() == 0){
            logger.info("returning 0");
            return;
        }

        List<UpdateRequest> updateRequests = new ArrayList<>();
        for(Node node : nodes){

            String collectionCheckAttribute = null;
            QueryBuilder collectionCheckQuery = null;
            /**
             * try it is a usage
             */
            QueryBuilder queryUsage = QueryBuilders.termQuery("collections.usagedbid",node.getId());
            SearchHits searchHitsIO = this.search(INDEX_WORKSPACE,queryUsage,0,1);
            if(searchHitsIO.getTotalHits().value > 0){
                collectionCheckAttribute = "usagedbid";
                collectionCheckQuery = queryUsage;
            }

            /**
             * try it is an collection
             */
            QueryBuilder queryCollection = QueryBuilders.termQuery("collections.dbid", node.getId());
            if(collectionCheckAttribute == null) {
                searchHitsIO = this.search(INDEX_WORKSPACE, queryCollection, 0, 1);
                if (searchHitsIO.getTotalHits().value > 0) {
                    collectionCheckAttribute = "dbid";
                    collectionCheckQuery = queryCollection;
                }
            }

            //nothing to cleanup
            if(collectionCheckAttribute == null){
                continue;
            }
            final String checkAtt = collectionCheckAttribute;
            logger.info("cleanup collection cause " + (collectionCheckAttribute.equals("dbid")? "collection deleted" : "usage deleted"));
            new SearchHitsRunner(this){
                @Override
                public void execute(SearchHit hitIO) throws IOException {
                    List<Map<String, Object>> collections = (List<Map<String, Object>>) hitIO.getSourceAsMap().get("collections");
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    {
                        builder.startArray("collections");
                        if (collections != null && collections.size() > 0) {
                            for (Map<String, Object> collection : collections) {
                                long nodeDbId = node.getId();

                                Object collCeckAttValue = collection.get(checkAtt);
                                if(collCeckAttValue == null){
                                    logger.error("replicated collection " + collection.get("dbid") + " does not have a property " + checkAtt +" will leave it out");
                                    continue;
                                }
                                long collectionAttValue = Long.parseLong(collCeckAttValue.toString());
                                if (nodeDbId != collectionAttValue) {
                                    builder.startObject();
                                    for (Map.Entry<String, Object> entry : collection.entrySet()) {
                                        builder.field(entry.getKey(), entry.getValue());
                                    }
                                    builder.endObject();
                                }
                            }
                        }
                        builder.endArray();
                    }
                    builder.endObject();
                    int dbid = Integer.parseInt(hitIO.getId());

                    UpdateRequest request = new UpdateRequest(
                            INDEX_WORKSPACE,
                            Long.toString(dbid)).doc(builder);
                    updateRequests.add(request);
                }
            }.run(collectionCheckQuery);
        }
        this.updateBulk(updateRequests);
        logger.info("returning");
    }

    /**
     * update ios collection metdata, that are containted inside a collection
     * @param nodeCollection
     * @throws IOException
     *
     * @deprecated
     */
    private void _onUpdateRefreshCollectionReplicas(Node nodeCollection) throws IOException {
        List<UpdateRequest> updateRequests = new ArrayList<>();
        QueryBuilder queryCollection = QueryBuilders.termQuery("collections.dbid", nodeCollection.getId());
        new SearchHitsRunner(this)
        {
            @Override
            public void execute(SearchHit hit) throws IOException{
                logger.info("updating collection data for:"+hit.getSourceAsMap().get("dbid"));
                QueryBuilder collectionQuery = QueryBuilders.termQuery("properties.sys:node-uuid",Tools.getUUID(nodeCollection.getNodeRef()));
                SearchHits searchHitsCollection = this.elasticClient.search(INDEX_WORKSPACE,collectionQuery,0,1);

                if(searchHitsCollection == null || searchHitsCollection.getTotalHits().value == 0){
                    return;
                }

                List<Map<String, Object>> collectionReplicas = (List<Map<String, Object>>) hit.getSourceAsMap().get("collections");
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.startArray("collections");

                    for (Map<String, Object> collectionReplica : collectionReplicas) {
                        long collReplDbid = ((Number)collectionReplica.get("dbid")).longValue();
                        if (collReplDbid == nodeCollection.getId()) {
                            builder.startObject();
                            for(Map.Entry<String, Object> entry : searchHitsCollection.getHits()[0].getSourceAsMap().entrySet()){
                                builder.field(entry.getKey(), entry.getValue());
                            }
                            builder.endObject();
                        }else{
                            builder.startObject();
                            for (Map.Entry<String, Object> entry : collectionReplica.entrySet()) {
                                builder.field(entry.getKey(), entry.getValue());
                            }
                            builder.endObject();
                        }
                    }

                    builder.endArray();
                }
                builder.endObject();
                int dbid = Integer.parseInt(hit.getId());
                UpdateRequest request = new UpdateRequest(
                        INDEX_WORKSPACE,
                        Long.toString(dbid)).doc(builder);
                updateRequests.add(request);

            }
        }.run(queryCollection);

        this.updateBulk(updateRequests);
    }

    private void onUpdateRefreshCollectionReplicas(NodeMetadata node) throws IOException {

        String query = null;
        if("ccm:map".equals(node.getType())){
            query = "properties.ccm:usagecourseid.keyword";
        }else if("ccm:io".equals(node.getType())){
            query = "properties.ccm:usageparentnodeid.keyword";
        }else{
            logger.info("can not handle collections for type:" + node.getType());
            return;
        }

        logger.info("updateing collections for " + node.getType() +" " +node.getId());

       //find usages for collection
        QueryBuilder queryUsages = QueryBuilders.termQuery(query, Tools.getUUID(node.getNodeRef()));
        new SearchHitsRunner(this){

            @Override
            public void execute(SearchHit hit) throws IOException {
                long usageDbId = ((Number)hit.getSourceAsMap().get("dbid")).longValue();
                GetNodeMetadataParam param = new GetNodeMetadataParam();
                param.setNodeIds(Arrays.asList(new Long[]{usageDbId}));
                NodeMetadatas nodeMetadatas = alfrescoClient.getNodeMetadata(param);
                if(nodeMetadatas == null || nodeMetadatas.getNodes() == null || nodeMetadatas.getNodes().size() == 0){
                    logger.error("could not find usage object in alfresco with dbid:" + usageDbId);
                    return;
                }
                NodeMetadata usage = nodeMetadatas.getNodes().get(0);
                logger.info("running indexCollections for usage: " + usageDbId);
                indexCollections(usage);
            }
        }.run(queryUsages);
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
                if("de".equals(m.get("locale")) || "de_".equals(m.get("locale"))){
                    value = (String)m.get("value");
                }
            }
            if(value == null) value = defaultValue;
            return value;
        }else if(listvalue.size() == 1){
            Map multilangValue = (Map) listvalue.get(0);
            return (String) multilangValue.get("value");
        }else{
            return null;
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
            transaction.setTxnId(((Number)resp.getSource().get("txnId")).longValue());
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
        logger.info("starting size:"+nodes.size());
        RestHighLevelClient client = getClient();

        BulkRequest bulkRequest = new BulkRequest(INDEX_WORKSPACE);
        for(Node node : nodes){

            DeleteRequest request = new DeleteRequest(
                    INDEX_WORKSPACE,
                    Long.toString(node.getId()));

            bulkRequest.add(request);
        }
        if(bulkRequest.numberOfActions() > 0){
            BulkResponse response = client.bulk(bulkRequest,RequestOptions.DEFAULT);
            for(BulkItemResponse item : response.getItems()){
                if(item.getFailure() != null){
                    logger.error(item.getFailureMessage());
                }
            }
        }

        client.close();
        logger.info("returning");
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

    public List<SearchHit> searchForAclIds(List<Long> aclIds) throws IOException {
        List<SearchHit> result = new ArrayList<>();
        // paritionate to prevent maxClauseCount errors in elastic
        for(int i=0;i<aclIds.size();i+=500){
            BoolQueryBuilder query = QueryBuilders.boolQuery().minimumShouldMatch(1);
            for(Long aclId: aclIds.subList(i, Math.min(aclIds.size(), i + 500))) {
                query.should(QueryBuilders.termQuery("aclId", aclId));
            }
            SearchHits hits = this.search(INDEX_WORKSPACE, query, 0, aclIds.size());
            result.addAll(Arrays.asList(hits.getHits()));
        }
        return result;
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
