package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.alfresco.client.NodePreview;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@Component
public class EduSharingClient {

    private static final Logger logger = LoggerFactory.getLogger(EduSharingClient.class);

    @Value("${alfresco.host}")
    String alfrescoHost;

    @Value("${alfresco.port}")
    String alfrescoPort;

    @Value("${alfresco.protocol}")
    String alfrescoProtocol;

    @Value("${alfresco.username}")
    String alfrescoUsername;

    @Value("${alfresco.password}")
    String alfrescoPassword;

    @Value("${log.requests}")
    String logRequests;


    Map<String,Set<String>> valuespaceProps = new HashMap<>();

    @Value("${valuespace.languages}")
    String[] valuespaceLanguages;

    @Value("${valuespace.cache.check.after.ms : 120000}")
    long valuespaceCacheCheckAfterMs = 120000;

    @Value("${tracker.fetchThumbnails}")
    boolean fetchThumbnails;

    long valuespaceCacheLastChecked = -1;

    long valuespaceCacheLastModified = -1;


    private Client educlient;

    private String authorizationHeader;

    String URL_MDS_VALUES = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}/values";

    String URL_PREVIEW = "/edu-sharing/preview?nodeId=${nodeId}&storeProtocol=${storeProtocol}&storeId=${storeId}&crop=true&maxWidth=${width}&maxHeight=${height}&quality=${quality}";

    String URL_MDS = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}";

    String URL_MDS_ALL = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-";

    String URL_ABOUT = "/edu-sharing/rest/_about";

    String URL_REPOSITORIES = "/edu-sharing/rest/network/v1/repositories";

    HashMap<String,HashMap<String, HashMap<String,ValuespaceEntries>>> cache = new HashMap<>();

    @PostConstruct
    public void init()  throws IOException {
        authorizationHeader = "Basic "
                + org.apache.cxf.common.util.Base64Utility.encode(String.format("%s:%s",alfrescoUsername,alfrescoPassword).getBytes());
        educlient = ClientBuilder.newBuilder()
                .register(JacksonJsonProvider.class)
                .register(PreviewDataReader.class).build();
        if (Boolean.parseBoolean(logRequests)) {
            educlient.register(new LoggingFeature());
        }
        educlient.property("use.async.http.conduit", Boolean.TRUE);
        educlient.property("org.apache.cxf.transport.http.async.usePolicy", AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);
        // relevant for external previews or static previews (e.g. svg)
        educlient.property("http.autoredirect", true);


        MetadataSets metadataSets = getMetadataSets();
        for(MetadataSet metadataSet : metadataSets.getMetadatasets()){
            Set<String> valueSpacePropsTmp = new HashSet<>();
            valueSpacePropsTmp.addAll(getValuespaceProperties(metadataSet.getId()));
            valuespaceProps.put(metadataSet.getId(),valueSpacePropsTmp);
            logger.info("added " + valueSpacePropsTmp.size() +" i18n props for mds: " + metadataSet.getId());
        }
    }

    public String translate(String mds, String language, String property, String key){
        ValuespaceEntries entries = getValuespace(mds,language,property);
        if(entries.getError() != null){
            logger.error("error:" + entries.getError() + " m:"+entries.getMessage());
            return null;
        }
        String result = null;
        for(ValuespaceEntry entry : entries.getValues()){
            if(entry.getKey().equals(key)){
                result = entry.getDisplayString();
            }
        }
        return result;
    }

    public void translateValuespaceProps(NodeData data){

        Map<String, Serializable> properties = data.getNodeMetadata().getProperties();

        String mds = (String)data.getNodeMetadata().getProperties().get(Constants.CM_PROP_EDUMETADATASET);
        if(mds == null) mds = "default";

        if(mds.equals("default")){
            //"default" in repo is hard coded, should map on the first registered mds in repo
            mds = valuespaceProps.keySet().iterator().next();
        }

        Set<String> valueSpacePropsMds = valuespaceProps.get(mds);
        if(valueSpacePropsMds == null){
            logger.error("no i18n props found for mds:" + mds);
            return;
        }

        for(Map.Entry<String, Serializable> prop : properties.entrySet()){
            String key = Constants.getValidLocalName(prop.getKey());
            if(key == null){
                logger.error("unknown namespace: " + prop.getKey());
                continue;
            }


            if(valueSpacePropsMds.contains(key)){
                for(String language : valuespaceLanguages) {
                    Serializable translated = null;

                    if(prop.getValue() == null) continue;

                    if (prop.getValue() instanceof List) {
                        ArrayList<String> translatedList = new ArrayList<>();
                        for (String value : (List<String>) prop.getValue()) {
                           String translatedVal =  translate(mds,language,key,value);
                           if(translatedVal != null && !translatedVal.trim().equals("")){
                               translatedList.add(translatedVal);
                           }
                        }
                        if(translatedList.size()>0){
                            translated = translatedList;
                        }
                    } else {
                        String translatedVal =  translate(mds,language,key,prop.getValue().toString());
                        if(translatedVal != null){
                            translated = translatedVal;
                        }
                    }

                    Map<String, List<String>> valuespacesForLanguage = data.getValueSpaces().get(language);
                    if(valuespacesForLanguage == null){
                        valuespacesForLanguage = new HashMap<>();
                        data.getValueSpaces().put(language,valuespacesForLanguage);
                    }
                    if(translated instanceof List){
                        valuespacesForLanguage.put(prop.getKey(),(List)translated);
                    }else{
                        valuespacesForLanguage.put(prop.getKey(),Arrays.asList(new String[]{(String)translated}));
                    }
                }
            }
        }



    }

    /**
     *
     * @param mds
     * @param language de-de, en-en
     * @param property
     * @return
     */
    public ValuespaceEntries getValuespace(String mds, String language, String property ){

        ValuespaceEntries entries = getValuespaceFromCache(mds, language, property);

        if(entries != null){
            logger.debug("got valuespace entries from cache");
            return entries;
        }

        GetValuesParameters params = new GetValuesParameters();
        GetValuesParameters.ValueParameters vp = new GetValuesParameters.ValueParameters();

        String url = new String(URL_MDS_VALUES);
        url = url.replace("${mds}",mds);
        url = getUrl(url);

        vp.setProperty(property);
        vp.setQuery("ngsearch");
        params.setValueParameters(vp);

        entries = educlient
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .header("locale",language)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .post(Entity.json(params)).readEntity(ValuespaceEntries.class);
        addValuespaceToCache(mds, language, property, entries);
        return entries;
    }

    public List<String> getValuespaceProperties(String mds){
        String url = new String(URL_MDS);
        url = url.replace("${mds}",mds);
        url = getUrl(url);
        MdsV2 mdsV2 = educlient
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
        .get().readEntity(MdsV2.class);

        List<String> result = new ArrayList<>();
        for(WidgetV2 widget : mdsV2.getWidgets()){
            if(widget.isHasValues()){
                result.add(widget.getId());
            }
        }
        return result;
    }

    public void addPreview(NodeData node){
        if(!fetchThumbnails){
            return;
        }
        String url = getUrl(URL_PREVIEW).
                replace("${nodeId}", Tools.getUUID(node.getNodeMetadata().getNodeRef())).
                replace("${storeProtocol}", Tools.getProtocol(node.getNodeMetadata().getNodeRef())).
                replace("${storeId}", Tools.getIdentifier(node.getNodeMetadata().getNodeRef()));

        String urlSmall = url.replace("${width}", "400").
                replace("${height}", "400").
                replace("${quality}", "60");
        String urlLarge = url.replace("${width}", "800").
                replace("${height}", "800").
                replace("${quality}", "70");

        PreviewData previewSmall=getPreviewData(urlSmall);
        //byte[] previewLarge=getPreviewData(urlSmall);

        NodePreview preview = new NodePreview();
        if(previewSmall!=null) {
            preview.setMimetype(previewSmall.getMimetype());
            preview.setSmall(previewSmall.getData());
        }

        // both are individual, so also save the small one
        /*
        if(!Arrays.equals(previewSmall, previewLarge)){
            preview.setLarge(preview);
        }

         */
        node.setNodePreview(preview);
    }

    private PreviewData getPreviewData(String url) {
        try {
            return educlient.target(url).
                    request(MediaType.WILDCARD).
                    header(HttpHeaders.AUTHORIZATION, authorizationHeader).
                    get().readEntity(PreviewData.class);
        }catch(Exception e) {
            logger.info("Could not fetch preview from " + url, e);
            return null;
        }
    }

    public About getAbout(){
        String url = new String(URL_ABOUT);
        url = getUrl(url);
        try {
            About about = educlient
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get().readEntity(About.class);
            return about;
        } catch(ProcessingException e){
            logger.error("Error while trying to fetch edu-sharing API at " + url + ". Make sure you're running edu-sharing >= 6.0", e);
            throw e;
        }
    }

    public MetadataSets getMetadataSets(){
        String url = new String(URL_MDS_ALL);
        url = getUrl(url);
        return educlient.target(url)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .accept(MediaType.APPLICATION_JSON).
                get().readEntity(MetadataSets.class);
    }

    public Repository getHomeRepository(){
        String url = new String(URL_REPOSITORIES);
        url = getUrl(url);
        Repositories repositories = educlient.target(url).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, authorizationHeader).
                get().readEntity(Repositories.class);
        for(Repository rep : repositories.getRepositories()){
            if(rep.isHomeRepo()) return rep;
        }
        return null;
    }


    /**
     * refreshes cache when necessary
     * use valuespace.cache.check.after.ms config to determine check frequence
     */
    public void refreshValuespaceCache(){
        if(valuespaceCacheLastChecked == -1
                || valuespaceCacheLastChecked < (System.currentTimeMillis() - valuespaceCacheCheckAfterMs) ){
            logger.info("will check if cache in edu-sharing changed");
            About about = getAbout();
            if(about.getLastCacheUpdate() > valuespaceCacheLastModified){
                logger.info("repos last cache updated" + new Date(about.getLastCacheUpdate())
                        +": force valuespace cache refresh");
                cache.clear();
                valuespaceCacheLastModified = about.getLastCacheUpdate();
            }
            valuespaceCacheLastChecked = System.currentTimeMillis();
        }

    }

    private String getUrl(String path){
        return alfrescoProtocol+"://"+alfrescoHost+":"+alfrescoPort+path;
    }

    private ValuespaceEntries getValuespaceFromCache(String mds, String language, String property){

        HashMap<String,HashMap<String,ValuespaceEntries>> mdsMap = cache.get(mds);
        if(mdsMap == null){
            return null;
        }

        Map<String,ValuespaceEntries> propMap = mdsMap.get(language);
        if(propMap == null){
            return null;
        }
        return propMap.get(property);
    }

    private void addValuespaceToCache(String mds, String language, String property, ValuespaceEntries entries){

        HashMap<String,HashMap<String,ValuespaceEntries>> mdsMap = cache.get(mds);
        if(mdsMap == null){
            mdsMap = new HashMap<>();
            cache.put(mds,mdsMap);
        }

        HashMap<String,ValuespaceEntries> propMap = mdsMap.get(language);
        if(propMap == null){
            propMap = new HashMap<>();
            mdsMap.put(language,propMap);
        }

        propMap.put(property,entries);
    }

}
