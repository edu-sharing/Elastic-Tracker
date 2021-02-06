package org.edu_sharing.elasticsearch;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Edu_SharingElasticsearchTracker {

    public static void main(String[] args) {
        SpringApplication.run(Edu_SharingElasticsearchTracker.class, args);
    }
}
