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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Transformation task for instrumenting the project's local classes.
 */
public abstract class ByteBuddyLocalClassesEnhancerTask extends DefaultTask {

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

    @TaskAction
    public void action() {
        transform(getOutputDir().get().getAsFile());
    }

    private void transform(File outputDir) {
        try (ClassFileLocator contextClassFileLocator = createContextClassFileLocator(getRuntimeClasspath().plus(getAndroidBootClasspath()).plus(getByteBuddyClasspath()).getFiles())) {
            makeEngine()
                    .with(contextClassFileLocator)
                    .apply(
                            createSource(),
                            new Plugin.Engine.Target.ForFolder(outputDir),
                            createPluginFactories()
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Plugin.Engine makeEngine() {
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersionString(getJavaTargetCompatibilityVersion()
                .get()
                .toString());
        MethodNameTransformer methodNameTransformer = MethodNameTransformer.Suffixing.withRandomSuffix();

        return Plugin.Engine.Default.of(new AndroidEntryPoint(), classFileVersion, methodNameTransformer);
    }

    private List<Plugin.Factory> createPluginFactories() throws IOException {
        URLClassLoader androidLoader = new URLClassLoader(toUrlArray(getAndroidBootClasspath().getFiles()), ByteBuddy.class.getClassLoader());
        URLClassLoader pluginLoader = new URLClassLoader(toUrlArray(getByteBuddyClasspath().getFiles()), androidLoader);
        ArrayList<Plugin.Factory> factories = new ArrayList<>();

        AndroidDescriptor androidDescriptor = new LocalAndroidDescriptor();
        BuildLogger buildLogger;
        try {
            buildLogger = (BuildLogger) Class.forName("net.bytebuddy.build.gradle.GradleBuildLogger")
                    .getConstructor(Logger.class)
                    .newInstance(getProject().getLogger());
        } catch (Exception exception) {
            throw new GradleException("Failed to resolve Gradle build logger", exception);
        }
        for (String className : Plugin.Engine.Default.scan(pluginLoader)) {
            factories.add(createFactoryFromClassName(className, pluginLoader, androidDescriptor, getProject().getLogger(), buildLogger));
        }

        return factories;
    }

    private Plugin.Factory.UsingReflection createFactoryFromClassName(
            String className,
            ClassLoader classLoader,
            AndroidDescriptor androidDescriptor,
            Logger gradleLogger,
            BuildLogger buildLogger
    ) {
        try {
            Class<? extends Plugin> pluginClass = getClassFromName(className, classLoader);
            return new Plugin.Factory.UsingReflection(pluginClass)
                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(AndroidDescriptor.class, androidDescriptor))
                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, gradleLogger))
                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(org.slf4j.Logger.class, gradleLogger))
                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, buildLogger));
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot resolve plugin: $className", t);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Plugin> getClassFromName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> type = Class.forName(className, false, classLoader);

        if (!Plugin.class.isAssignableFrom(type)) {
            throw new GradleException(type.getName() + " does not implement " + Plugin.class.getName());
        }

        return (Class<? extends Plugin>) type;
    }

    private URL[] toUrlArray(Set<File> files) {
        List<URL> urls = new ArrayList<>();
        files.forEach(file -> {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
        return urls.toArray(new URL[0]);
    }

    private CompoundSourceOrigin createSource() {
        List<Directory> directories = getLocalClassesDirs().get();
        Set<Plugin.Engine.Source.Origin> origins = new HashSet<>();
        for (Directory directory : directories) {
            origins.add(new Plugin.Engine.Source.ForFolder(directory.getAsFile()));
        }
        return new CompoundSourceOrigin(origins);
    }

    private ClassFileLocator createContextClassFileLocator(Set<File> filesAndDirs) throws IOException {
        ArrayList<ClassFileLocator> classFileLocators = new ArrayList<>();

        for (File artifact : filesAndDirs) {
            classFileLocators.add(artifact.isFile() ? ClassFileLocator.ForJarFile.of(artifact) : new ClassFileLocator.ForFolder(artifact));
        }

        classFileLocators.add(
                ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader())
        );

        return new ClassFileLocator.Compound(classFileLocators);
    }

    /**
     * An implementation for an Android descriptor for local classpath queries.
     */
    protected static class LocalAndroidDescriptor implements AndroidDescriptor {

        /**
         * Returns the LOCAL {@link net.bytebuddy.build.AndroidDescriptor.TypeScope}.
         *
         * @return the LOCAL {@link net.bytebuddy.build.AndroidDescriptor.TypeScope}.
         */
        @Override
        public TypeScope getTypeScope(TypeDescription typeDescription) {
            return TypeScope.LOCAL;
        }
    }

    private static class AndroidEntryPoint implements EntryPoint {

        @Override
        public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
            return new ByteBuddy(classFileVersion).with(TypeValidation.DISABLED);
        }

        @Override
        public DynamicType.Builder<?> transform(
                TypeDescription typeDescription,
                ByteBuddy byteBuddy,
                ClassFileLocator classFileLocator,
                MethodNameTransformer methodNameTransformer
        ) {
            return byteBuddy.decorate(typeDescription, classFileLocator);
        }
    }

    /**
     * A configuration action for the {@link ByteBuddyLocalClassesEnhancerTask} task.
     */
    protected static class ConfigurationAction implements Action<ByteBuddyLocalClassesEnhancerTask> {
        /**
         * The current variant Byte Buddy configuration.
         */
        private final Configuration bytebuddyClasspath;
        /**
         * The android gradle extension.
         */
        private final BaseExtension androidExtension;
        /**
         * The current variant's runtime classpath.
         */
        private final FileCollection runtimeClasspath;

        /**
         * @param bytebuddyClasspath The current variant Byte Buddy configuration.
         * @param androidExtension   The android gradle extension.
         * @param runtimeClasspath   The current variant's runtime classpath.
         */
        public ConfigurationAction(Configuration bytebuddyClasspath, BaseExtension androidExtension, FileCollection runtimeClasspath) {
            this.bytebuddyClasspath = bytebuddyClasspath;
            this.androidExtension = androidExtension;
            this.runtimeClasspath = runtimeClasspath;
        }

        @Override
        public void execute(ByteBuddyLocalClassesEnhancerTask task) {
            task.getByteBuddyClasspath().from(bytebuddyClasspath);
            task.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
            task.getRuntimeClasspath().from(runtimeClasspath);
            task.getJavaTargetCompatibilityVersion().set(androidExtension.getCompileOptions().getTargetCompatibility());
        }
    }
}
