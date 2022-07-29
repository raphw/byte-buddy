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
package net.bytebuddy.build.gradle.android.connector.adapter.current.task;

import com.android.build.gradle.AppExtension;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import net.bytebuddy.build.gradle.android.utils.Many;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

public abstract class TransformAppTask extends DefaultTask {

    private final Args args;

    @Inject
    public TransformAppTask(Args args) {
        this.args = args;
    }

    @InputFiles
    abstract public ListProperty<Directory> getAllClasses();

    @InputFiles
    abstract public ListProperty<RegularFile> getAllJars();

    @InputFiles
    abstract public ConfigurableFileCollection getBytebuddyDependencies();

    @OutputFiles
    abstract public DirectoryProperty getOutput();

    @TaskAction
    public void transform() {
        List<File> allClassesDirs = Many.map(getAllClasses().get(), Directory::getAsFile);
        List<File> allJarFiles = Many.map(getAllJars().get(), RegularFile::getAsFile);

        AndroidTransformation.Input input = new AndroidTransformation.Input(
                getTargetClasspath(allClassesDirs, allJarFiles),
                getReferenceClasspath(),
                getAndroidBootClasspath(),
                getBytebuddyDependencies().getFiles(),
                getJavaTargetVersion()
        );

        args.transformation.transform(input, getOutput().get().getAsFile());
    }

    private int getJavaTargetVersion() {
        return Integer.parseInt(args.appExtension.getCompileOptions().getTargetCompatibility().getMajorVersion());
    }

    private Set<File> getAndroidBootClasspath() {
        return Many.toSet(args.appExtension.getBootClasspath());
    }

    private Set<File> getReferenceClasspath() {
        return emptySet();
    }

    private Set<File> getTargetClasspath(
            List<File> allClassesDirs,
            List<File> allJarFiles
    ) {
        Set<File> target = new HashSet<>();
        target.addAll(allClassesDirs);
        target.addAll(allJarFiles);
        return target;
    }

    public static class Args {
        final AndroidTransformation transformation;
        final AppExtension appExtension;

        public Args(AndroidTransformation transformation, AppExtension appExtension) {
            this.transformation = transformation;
            this.appExtension = appExtension;
        }
    }
}