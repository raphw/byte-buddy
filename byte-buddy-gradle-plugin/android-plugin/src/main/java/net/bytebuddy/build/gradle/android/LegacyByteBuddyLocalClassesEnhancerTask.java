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
package net.bytebuddy.build.gradle.android;

import com.android.build.gradle.BaseExtension;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AndroidDescriptor;
import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Transformation task for instrumenting the project's local classes using the legacy AGP api < 7.4.0.
 */
public abstract class LegacyByteBuddyLocalClassesEnhancerTask extends DefaultTask {

    /**
     * Returns the boot class path of Android.
     *
     * @return The boot class path of Android.
     */
    @InputFiles
    public abstract ConfigurableFileCollection getAndroidBootClasspath();

    /**
     * Returns Byte Buddy's class path.
     *
     * @return Byte Buddy's class path.
     */
    @InputFiles
    public abstract ConfigurableFileCollection getByteBuddyClasspath();

    /**
     * Returns the runtime class path.
     *
     * @return The runtime class path.
     */
    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    /**
     * Returns the Java target compatibility version.
     *
     * @return The Java target compatibility version.
     */
    @Input
    public abstract Property<JavaVersion> getJavaTargetCompatibilityVersion();

    /**
     * Target project's local classes dirs.
     *
     * @return The target project's local classes dirs.
     */
    @InputFiles
    public abstract ListProperty<Directory> getLocalClassesDirs();

    /**
     * The instrumented classes destination dir.
     *
     * @return The instrumented classes destination dir.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /**
     * Translates a collection of files to {@link URL}s.
     *
     * @param files The list of files to translate.
     * @return An array of URLs representing the provided files.
     */
    private static URL[] toUrls(Collection<File> files) {
        URL[] url = new URL[files.size()];
        int index = 0;
        for (File file : files) {
            try {
                url[index++] = file.toURI().toURL();
            } catch (MalformedURLException exception) {
                throw new IllegalStateException("Failed to convert file " + file.getAbsolutePath(), exception);
            }
        }
        return url;
    }

    /**
     * Executes the plugin for transforming local classes.
     */
    @TaskAction
    public void execute() {
        try {
            ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersionString(getJavaTargetCompatibilityVersion().get().toString());
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (File file : getRuntimeClasspath().plus(getAndroidBootClasspath()).plus(getByteBuddyClasspath()).getFiles()) {
                classFileLocators.add(file.isFile()
                        ? ClassFileLocator.ForJarFile.of(file, classFileVersion)
                        : ClassFileLocator.ForFolder.of(file, classFileVersion));
            }
            classFileLocators.add(ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader()));
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            try {
                Set<Plugin.Engine.Source> sources = new LinkedHashSet<Plugin.Engine.Source>();
                for (Directory directory : getLocalClassesDirs().get()) {
                    sources.add(new Plugin.Engine.Source.ForFolder(directory.getAsFile()));
                }
                ClassLoader classLoader = new URLClassLoader(
                        toUrls(getByteBuddyClasspath().getFiles()),
                        new URLClassLoader(toUrls(getAndroidBootClasspath().getFiles()), ByteBuddy.class.getClassLoader()));
                try {
                    List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>();
                    BuildLogger buildLogger;
                    try {
                        buildLogger = (BuildLogger) Class.forName("net.bytebuddy.build.gradle.GradleBuildLogger")
                                .getConstructor(Logger.class)
                                .newInstance(getProject().getLogger());
                    } catch (Exception exception) {
                        throw new GradleException("Failed to resolve Gradle build logger", exception);
                    }
                    for (String name : Plugin.Engine.Default.scan(classLoader)) {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<? extends Plugin> type = (Class<? extends Plugin>) Class.forName(name, false, classLoader);
                            if (!Plugin.class.isAssignableFrom(type)) {
                                throw new GradleException(type.getName() + " does not implement " + Plugin.class.getName());
                            }
                            factories.add(new Plugin.Factory.UsingReflection(type)
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(AndroidDescriptor.class, AndroidDescriptor.Trivial.LOCAL))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, getProject().getLogger()))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(org.slf4j.Logger.class, getProject().getLogger()))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, buildLogger)));
                        } catch (Throwable throwable) {
                            throw new IllegalStateException("Cannot resolve plugin: " + name, throwable);
                        }
                    }
                    Plugin.Engine.Summary summary = Plugin.Engine.Default.of(new EntryPoint.Unvalidated(EntryPoint.Default.DECORATE),
                                    classFileVersion,
                                    MethodNameTransformer.Suffixing.withRandomSuffix())
                            .with(classFileLocator)
                            .with(classFileVersion)
                            .apply(new Plugin.Engine.Source.Compound(sources), new Plugin.Engine.Target.ForFolder(getOutputDir().get().getAsFile()), factories);
                    if (!summary.getFailed().isEmpty()) {
                        throw new IllegalStateException(summary.getFailed() + " local type transformations have failed");
                    } else if (summary.getTransformed().isEmpty()) {
                        getLogger().info("No local types were transformed during plugin execution");
                    } else {
                        getLogger().info("Transformed {} local type(s)", summary.getTransformed().size());
                    }
                } finally {
                    if (classLoader instanceof Closeable) {
                        ((Closeable) classLoader).close();
                    }
                    if (classLoader.getParent() instanceof Closeable) {
                        ((Closeable) classLoader.getParent()).close();
                    }
                }
            } finally {
                classFileLocator.close();
            }
        } catch (IOException exception) {
            throw new GradleException("Failed to transform local classes", exception);
        }
    }

    /**
     * A configuration action for the {@link LegacyByteBuddyLocalClassesEnhancerTask} task.
     */
    public static class ConfigurationAction implements Action<LegacyByteBuddyLocalClassesEnhancerTask> {

        /**
         * The current variant's Byte Buddy configuration.
         */
        private final FileCollection byteBuddyConfiguration;

        /**
         * The android gradle extension.
         */
        private final BaseExtension androidExtension;

        /**
         * The current variant's runtime classpath.
         */
        private final FileCollection runtimeClasspath;

        /**
         * @param byteBuddyConfiguration The current variant Byte Buddy configuration.
         * @param androidExtension       The android gradle extension.
         * @param runtimeClasspath       The current variant's runtime classpath.
         */
        public ConfigurationAction(FileCollection byteBuddyConfiguration, BaseExtension androidExtension, FileCollection runtimeClasspath) {
            this.byteBuddyConfiguration = byteBuddyConfiguration;
            this.androidExtension = androidExtension;
            this.runtimeClasspath = runtimeClasspath;
        }

        @Override
        public void execute(LegacyByteBuddyLocalClassesEnhancerTask task) {
            task.getByteBuddyClasspath().from(byteBuddyConfiguration);
            task.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
            task.getRuntimeClasspath().from(runtimeClasspath);
            task.getJavaTargetCompatibilityVersion().set(androidExtension.getCompileOptions().getTargetCompatibility());
        }
    }
}
