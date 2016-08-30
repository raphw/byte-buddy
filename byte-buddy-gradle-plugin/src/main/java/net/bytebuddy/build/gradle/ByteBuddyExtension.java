package net.bytebuddy.build.gradle;

import java.util.List;

public class ByteBuddyExtension {

    private List<Transformation> transformations;

    private Initialization initialization;

    private String suffix;

    private boolean failOnLiveInitializer;

    public ByteBuddyExtension() {
        failOnLiveInitializer = true;
    }

    public List<Transformation> getTransformations() {
        return transformations;
    }

    public void setTransformations(List<Transformation> transformations) {
        this.transformations = transformations;
    }

    public Initialization getInitialization() {
        return initialization;
    }

    public void setInitialization(Initialization initialization) {
        this.initialization = initialization;
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
