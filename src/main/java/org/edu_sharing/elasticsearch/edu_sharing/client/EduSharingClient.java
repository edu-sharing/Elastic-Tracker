package org.edu_sharing.elasticsearch.edu_sharing.client;

import org.apache.logging.log4j.LogManager;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class EduSharingClient {

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


    String[] valuespaceProps;

    @Value("${valuespace.languages}")
    String[] valuespaceLanguages;

    @Value("${valuespace.cache.check.after.ms : 120000}")
    long valuespaceCacheCheckAfterMs = 120000;

    long valuespaceCacheLastChecked = -1;

    long valuespaceCacheLastModified = -1;


    private Client client;

    String URL_MDS_VALUES = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}/values";

    String URL_MDS = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}";

    String URL_MDS_ALL = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-";

    String URL_ABOUT = "/edu-sharing/rest/_about";

    String URL_REPOSITORIES = "/edu-sharing/rest/network/v1/repositories";

    HashMap<String,HashMap<String, HashMap<String,ValuespaceEntries>>> cache = new HashMap<>();

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(EduSharingClient.class);


    public EduSharingClient(){
        Logger logger = Logger.getLogger(getClass().getName());
        Feature feature = new LoggingFeature(logger, Level.FINEST, null, null);
        client = ClientBuilder.newBuilder().register(feature).build();
    }

    @PostConstruct
    public void init()  throws IOException {

        MetadataSets metadataSets = getMetadataSets();

        Set<String> valueSpacePropsTmp = new HashSet<>();
        for(MetadataSet metadataSet : metadataSets.getMetadatasets()){
            valueSpacePropsTmp.addAll(getValuespaceProperties(metadataSet.getId()));
        }

        valuespaceProps = valueSpacePropsTmp.toArray(new String[]{});
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
        
        for(Map.Entry<String, Serializable> prop : properties.entrySet()){
            String key = Constants.getValidLocalName(prop.getKey());
            if(key == null){
                logger.error("unknown namespace: " + prop.getKey());
                continue;
            }

            if(Arrays.asList(valuespaceProps).contains(key)){
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

        entries = client
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .header("locale",language)
                .header(HttpHeaders.AUTHORIZATION, Tools.getBasicAuth(alfrescoUsername,alfrescoPassword))
                .post(Entity.json(params)).readEntity(ValuespaceEntries.class);
        addValuespaceToCache(mds, language, property, entries);
        return entries;
    }

    public List<String> getValuespaceProperties(String mds){
        String url = new String(URL_MDS);
        url = url.replace("${mds}",mds);
        url = getUrl(url);
        MdsV2 mdsV2 = client
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, Tools.getBasicAuth(alfrescoUsername,alfrescoPassword))
        .get().readEntity(MdsV2.class);

        List<String> result = new ArrayList<>();
        for(WidgetV2 widget : mdsV2.getWidgets()){
            if(widget.isHasValues()){
                result.add(widget.getId());
            }
        }
        return result;
    }

    public About getAbout(){
        String url = new String(URL_ABOUT);
        url = getUrl(url);
        About about = client
                .target(url)
                .request(MediaType.APPLICATION_JSON)
                .get().readEntity(About.class);
        return about;
    }

    public MetadataSets getMetadataSets(){
        String url = new String(URL_MDS_ALL);
        url = getUrl(url);
        MetadataSets mdss = client.target(url).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, Tools.getBasicAuth(alfrescoUsername,alfrescoPassword)).
                get().readEntity(MetadataSets.class);
        return mdss;
    }

    public Repository getHomeRepository(){
        String url = new String(URL_REPOSITORIES);
        url = getUrl(url);
        Repositories repositories = client.target(url).
                request(MediaType.APPLICATION_JSON).
                header(HttpHeaders.AUTHORIZATION, Tools.getBasicAuth(alfrescoUsername,alfrescoPassword)).
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