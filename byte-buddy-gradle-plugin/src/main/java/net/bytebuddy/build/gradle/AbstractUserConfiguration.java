package net.bytebuddy.build.gradle;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

public class AbstractUserConfiguration {

    private final Project project;

    private Artifact artifact;

    public AbstractUserConfiguration(Project project) {
        this.project = project;
    }

    public Artifact artifact(Closure<?> closure) {
        if (artifact != null) {
            throw new GradleException("Artifact is already set");
        }
        Artifact artifact = (Artifact) project.configure(new Artifact(), closure);
        this.artifact = artifact;
        return artifact;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public MavenCoordinate asCoordinate(Project project) {
        return new MavenCoordinate(artifact.getGroupId(project), artifact.getArtifactId(project), artifact.getVersion(project));
    }
}
