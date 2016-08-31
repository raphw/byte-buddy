package net.bytebuddy.build.gradle;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

public class ByteBuddyExtension {

    private final Project project;

    private List<Transformation> transformations;

    private Initialization initialization;

    private String suffix;

    private boolean failOnLiveInitializer;

    public ByteBuddyExtension(Project project) {
        this.project = project;
        transformations = new ArrayList<Transformation>();
        failOnLiveInitializer = true;
    }

    public Transformation transformation(Closure<?> closure) {
        Transformation transformation = (Transformation) project.configure(new Transformation(), closure);
        transformations.add(transformation);
        return transformation;
    }

    public Initialization initialization(Closure<?> closure) {
        if (initialization != null) {
            throw new GradleException("Initialization is already set");
        }
        Initialization initialization = (Initialization) project.configure(new Initialization(), closure);
        this.initialization = initialization;
        return initialization;
    }

    public List<Transformation> getTransformations() {
        return transformations;
    }

    public Initialization getInitialization() {
        return initialization == null ? Initialization.makeDefault() : initialization;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean isFailOnLiveInitializer() {
        return failOnLiveInitializer;
    }

    public void setFailOnLiveInitializer(boolean failOnLiveInitializer) {
        this.failOnLiveInitializer = failOnLiveInitializer;
    }
}
