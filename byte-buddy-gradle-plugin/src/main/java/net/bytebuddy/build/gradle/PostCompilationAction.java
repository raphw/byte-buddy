package net.bytebuddy.build.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.AbstractCompile;

public class PostCompilationAction implements Action<AbstractCompile> {

    private final Project project;

    private final ByteBuddyExtension byteBuddyExtension;

    protected PostCompilationAction(Project project, ByteBuddyExtension byteBuddyExtension) {
        this.project = project;
        this.byteBuddyExtension = byteBuddyExtension;
    }

    public static Action<AbstractCompile> of(Project project) {
        return new PostCompilationAction(project, project.getExtensions().create("byteBuddy", ByteBuddyExtension.class, project));
    }

    @Override
    public void execute(AbstractCompile compileTask) {
        compileTask.doLast(new TransformationAction(project, byteBuddyExtension, compileTask));
    }
}