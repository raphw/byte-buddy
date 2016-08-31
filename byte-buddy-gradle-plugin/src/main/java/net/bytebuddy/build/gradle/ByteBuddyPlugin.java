package net.bytebuddy.build.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.File;
import java.io.IOException;

public class ByteBuddyPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        final ByteBuddyExtension byteBuddyExtension = project.getExtensions()
                .create("byteBuddy", ByteBuddyExtension.class, project);
        project.getTasks().withType(AbstractCompile.class, new Action<AbstractCompile>() {
            @Override
            public void execute(AbstractCompile compileTask) {
                final Iterable<File> compileClasspathFiles = compileTask.getClasspath();
                final File classesDir = compileTask.getDestinationDir();
                compileTask.doLast(new Action<Task>() {
                    @Override
                    public void execute(Task task) {
                        try {
                            new Transformer(project, byteBuddyExtension)
                                    .processOutputDirectory(classesDir,
                                            compileClasspathFiles);
                        } catch (IOException e) {
                            throw new GradleException(
                                    "Exception in byte-buddy processing", e);
                        }
                    }
                });
            }
        });
    }
}
