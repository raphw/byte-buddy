package net.bytebuddy.build.gradle;

import org.gradle.api.Project;

public class Transformation extends AbstractUserConfiguration {

    private String plugin;

    public Transformation(Project project) {
        super(project);
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
