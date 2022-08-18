package net.bytebuddy.build.gradle.android.utils;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class LocalClassesSync extends DefaultTask {

    @Incremental
    @Classpath
    public abstract ConfigurableFileCollection getLocalClasspath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void execute(InputChanges inputChanges) {
        for (FileChange fileChange : inputChanges.getFileChanges(getLocalClasspath())) {
            if (fileChange.getFileType() == FileType.DIRECTORY) {
                return;
            }

            File target = getOutputDir().file(fileChange.getNormalizedPath()).get().getAsFile();
            switch (fileChange.getChangeType()) {
                case REMOVED:
                    target.delete();
                    break;
                case MODIFIED:
                    target.delete();
                default:
                    copyfile(fileChange.getFile(), target);
                    break;
            }
        }
    }

    private void copyfile(File from, File into) {
        try {
            if (!into.getParentFile().exists()) {
                into.getParentFile().mkdirs();
            }
            Files.copy(from.toPath(), into.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
