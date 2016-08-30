package net.bytebuddy.build.gradle;

import org.gradle.api.Project;

public class Artifact {

    private String group;

    private String name;

    private String version;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId(Project project) {
        return group == null
                ? project.getGroup().toString()
                : group;
    }

    public String getArtifactId(Project project) {
        return name == null
                ? project.getName()
                : name;
    }

    public String getVersion(Project project) {
        return version == null
                ? project.getVersion().toString()
                : version;
    }
}
