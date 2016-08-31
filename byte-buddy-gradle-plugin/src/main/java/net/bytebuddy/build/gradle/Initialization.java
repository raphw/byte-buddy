package net.bytebuddy.build.gradle;

import net.bytebuddy.build.EntryPoint;
import org.gradle.api.GradleException;

import java.io.File;

/**
 * Defines a configuration for a Maven build's type transformation.
 */
public class Initialization extends AbstractUserConfiguration {

    private String entryPoint;

    public static Initialization makeDefault() {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(EntryPoint.Default.REBASE.name());
        return initialization;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public EntryPoint toEntryPoint(ClassLoaderResolver classLoaderResolver, File root, Iterable<? extends File> classPath) {
        if (entryPoint == null || entryPoint.isEmpty()) {
            throw new GradleException("Entry point name is not defined");
        }
        for (EntryPoint.Default entryPoint : EntryPoint.Default.values()) {
            if (this.entryPoint.equals(entryPoint.name())) {
                return entryPoint;
            }
        }
        try {
            return (EntryPoint) Class.forName(entryPoint, false, classLoaderResolver.resolve(getClassPath(root, classPath)))
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception exception) {
            throw new GradleException("Cannot create entry point: " + entryPoint, exception);
        }
    }
}
