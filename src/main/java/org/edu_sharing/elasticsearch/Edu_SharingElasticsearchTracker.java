package org.edu_sharing.elasticsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class Edu_SharingElasticsearchTracker {

    @Autowired CLI cli;
    private static String[] args;
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Edu_SharingElasticsearchTracker.args = args;
        context = SpringApplication.run(Edu_SharingElasticsearchTracker.class, args);
        context.getBean(Edu_SharingElasticsearchTracker.class).init();
    }

    public void init() {
        CommandLine cli = new CommandLine(this.cli);
        cli.setUnmatchedArgumentsAllowed(true);
        if(cli.execute(args) == 0){
            System.out.println(context);
            SpringApplication.exit(context);
            System.exit(0);
        }
    }
}
