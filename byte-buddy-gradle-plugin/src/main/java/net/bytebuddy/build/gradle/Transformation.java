package net.bytebuddy.build.gradle;

import org.gradle.api.GradleException;

public class Transformation extends AbstractUserConfiguration {

    private String plugin;

    public String getPlugin() {
        if (plugin == null || plugin.isEmpty()) {
            throw new GradleException("Plugin name was not specified or is empty");
        }
        return plugin;
    }

    public String getRawPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
