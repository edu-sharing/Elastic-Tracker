package org.edu_sharing.elasticsearch.edu_sharing.client;

import org.apache.logging.log4j.LogManager;
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
import java.util.HashMap;
import java.util.Map;
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

    private Client client;

    String URL_MDS_VALUES = "/edu-sharing/rest/mds/v1/metadatasetsV2/-home-/${mds}/values";

    //map<mds,map<language,map<property,valuespacentries>>>
    HashMap<String,HashMap<String, HashMap<String,ValuespaceEntries>>> cache = new HashMap<>();

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(EduSharingClient.class);

    public EduSharingClient(){
        Logger logger = Logger.getLogger(getClass().getName());
        Feature feature = new LoggingFeature(logger, Level.FINEST, null, null);
        client = ClientBuilder.newBuilder().register(feature).build();
    }

    /**
     *
     * @param language de-de, en-en
     * @param property
     * @param mds
     * @return
     */
    public ValuespaceEntries getValuespace(String language, String property, String mds){

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