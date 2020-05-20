package org.edu_sharing.elasticsearch.edu_sharing.client;

import org.apache.logging.log4j.LogManager;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.tools.Constants;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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

    @Value("${valuespace.props}")
    String[] valuespaceProps;

    @Value("${valuespace.languages}")
    String[] valuespaceLanguages;


    private Client client;

    String URL_MDS_VALUES = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}/values";

    HashMap<String,HashMap<String, HashMap<String,ValuespaceEntries>>> cache = new HashMap<>();

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(EduSharingClient.class);

    public EduSharingClient(){
        Logger logger = Logger.getLogger(getClass().getName());
        Feature feature = new LoggingFeature(logger, Level.FINEST, null, null);
        client = ClientBuilder.newBuilder().register(feature).build();
    }

    public String translate(String mds, String language, String property, String key){
        ValuespaceEntries entries = getValuespace(mds,language,property);
        String result = null;
        for(ValuespaceEntry entry : entries.getValues()){
            if(entry.getKey().equals(key)){
                result = entry.getDisplayString();
            }
        }
        return result;
    }

    public void translateValuespaceProps(NodeData data){

        for(String v : this.valuespaceProps){
            System.out.println("valueSpaceProp:" + v);
        }
        for(String v : this.valuespaceLanguages){
            System.out.println("valuespaceLanguage:" + v);
        }

        Map<String, Serializable> properties = data.getNodeMetadata().getProperties();

        String mds = (String)data.getNodeMetadata().getProperties().get(Constants.CM_PROP_EDUMETADATASET);
        if(mds == null) mds = "default";

        Map<String,Serializable> translatedProps = new HashMap<>();
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
                    translatedProps.put(prop.getKey() + "_" + language, translated);
                }
            }
        }
        if(translatedProps.size() > 0){
            properties.putAll(translatedProps);
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
            logger.info("got valuespace entries from cache");
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
                .header("Accept-Language",language)
                .header(HttpHeaders.AUTHORIZATION, Tools.getBasicAuth(alfrescoUsername,alfrescoPassword))
                .post(Entity.json(params)).readEntity(ValuespaceEntries.class);
        addValuespaceToCache(mds, language, property, entries);
        return entries;
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