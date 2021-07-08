This application fetches data via the Alfresco/Solr API and pushes them into an elasticsearch index.

# Requirements:
   - Alfresco 5.2 with edu-sharing 6.0 or newer
   - Elasticsearch with version 7.13.x

# Configuration

1. 
   Please create `application.properties` next to your jar-location
   
   Check the included file for sample values. Usually, the following values need to be configured:
    ```
    alfresco.host=<alfresco-server-domain-or-ip>
    alfresco.port=8080
    alfresco.username=admin
    alfresco.password=<your-alfresco-admin-password>
    
    elastic.host=localhost
    elastic.port=9200
    ```
2. The tracker does currently only support unencrypted communication with the alfresco solr api.
   
   Therefore, the communication needs to be configured for both alfresco and the (still existing) solr:
   
    `alfresco-global.properties`:
   
       solr.secureComms=none
    
    `workspace-SpacesStore/conf/solrcore.properties` & `archive-SpacesStore/conf/solrcore.properties`:
   
       alfresco.secureComms=none

3. In case you want to install the tracker as a service on linux, we recommend creating an appropriate unit in systemd:
   
   `/etc/systemd/system/elasticsearch.tracker.service`

    ```
    [Unit]
    Description=elasticsearch.tracker
    After=syslog.target
    
    [Service]
    WorkingDirectory=/opt/elastic-tracker
    User=elasticsearch
    ExecStart=/usr/bin/java -jar /opt/elastic-tracker/edu_sharing-community-repository-backend-search-elastic-tracker-master-SNAPSHOT.jar
    SuccessExitStatus=143
    
    [Install]
    WantedBy=multi-user.target
   ```