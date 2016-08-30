package net.bytebuddy.build.gradle;

import org.apache.maven.plugin.MojoExecutionException;

public class Transformation extends AbstractUserConfiguration {

    private String plugin;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
