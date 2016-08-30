package net.bytebuddy.build.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ByteBuddyPlugin implements Plugin<Project> {

    /*
     * TODO:
     *
     * 1. Create complex configuration (allow nesting and lists).
     * 2. Find way to append task to class compilation / test-class compilation.
     * 3. Integrate Aether and read (Maven?) repositories from user configuration.
     * 4. Read class path / test class path.
     *
     * Bonus:
     * 1. Read explicit dependencies from Maven POM file instead of duplication in build.gradle.
     * 2. Are main and test folders of build target customizable locations?
     * 3. What GradleExceptions should be thrown? Any sub types?
     */

    @Override
    public void apply(Project project) {
        project.getExtensions().create("byteBuddy", ByteBuddyExtension.class, project);
        project.getTasks().create("transform", TransformTask.ForProductionTypes.class);
        project.getTasks().create("test-transform", TransformTask.ForTestTypes.class);
    }
}
