package net.bytebuddy.build.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.AbstractCompile;

/**
 * A Byte Buddy plugin that appends transformations to all compilation tasks.
 */
public class ByteBuddyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().withType(AbstractCompile.class, PostCompilationAction.of(project));
    }
}
