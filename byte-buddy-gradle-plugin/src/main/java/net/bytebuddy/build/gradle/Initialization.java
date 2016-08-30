package net.bytebuddy.build.gradle;

import net.bytebuddy.build.EntryPoint;
import org.apache.maven.plugin.MojoExecutionException;
import org.gradle.api.Project;

/**
 * Defines a configuration for a Maven build's type transformation.
 */
public class Initialization extends AbstractUserConfiguration {

    private String entryPoint;

    public Initialization(Project project) {
        super(project);
    }

    public static Initialization makeDefault(Project project) {
        Initialization initialization = new Initialization(project);
        initialization.setEntryPoint(EntryPoint.Default.REBASE.name());
        return initialization;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public EntryPoint toEntryPoint(ClassLoaderResolver classLoaderResolver) {
        return null; // TODO
    }
}
