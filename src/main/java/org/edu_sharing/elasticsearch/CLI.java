package org.edu_sharing.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(helpCommand = false, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Callable<Integer>
{
    @Value("${git.version}")
    String gitVersion;

    @Override
    public Integer call() throws Exception {
        // nothing to do, we use the mixinStandardHelpOptions
        return -1;
    }

}
