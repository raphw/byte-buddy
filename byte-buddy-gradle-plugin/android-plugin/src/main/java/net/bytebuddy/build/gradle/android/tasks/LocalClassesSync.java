/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build.gradle.android.tasks;

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

/**
 * Collects locally compiled classes (from both Java and Kotlin compilers) and places them in a single output
 * that will be later used as input for ByteBuddy.
 */
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
