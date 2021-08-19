package org.edu_sharing.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class Edu_SharingElasticsearchTracker {

    public static void main(String[] args) {
        CommandLine cli = new CommandLine(new CLI());
        cli.setUnmatchedArgumentsAllowed(true);
        if(cli.execute(args) == -1){
            SpringApplication.run(Edu_SharingElasticsearchTracker.class, args);
        }
    }
}
