package org.edu_sharing.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@Component
public class VersionProvider implements CommandLine.IVersionProvider {
    @Value("${alfresco.host}")
    String gitVersion;

    @Override
    public String[] getVersion() throws Exception {
        try (
                InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("git.properties");
        ) {
            Properties prop = new Properties();
            prop.load(stream);
            StringBuffer buffer = new StringBuffer();
            prop.entrySet().forEach((entry -> buffer.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n")));
            return new String[]{buffer.toString()};
        }
    }

    public VersionProvider() {
    }
}
