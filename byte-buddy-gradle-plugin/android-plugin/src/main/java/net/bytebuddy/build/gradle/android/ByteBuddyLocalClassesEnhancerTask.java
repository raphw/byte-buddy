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
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.QueueFactory;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Transformation task for instrumenting the project's local and dependencies' classes.
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
     * Returns the Java target compatibility version.
     *
     * @return The Java target compatibility version.
     */
    @Input
    public abstract Property<JavaVersion> getJavaTargetCompatibilityVersion();

    /**
     * Target project's local and dependencies jars.
     *
     * @return The target project's local and dependencies jars.
     */
    @InputFiles
    public abstract ListProperty<RegularFile> getInputJars();

    /**
     * Target project's local classes dirs.
     *
     * @return The target project's local classes dirs.
     */
    @InputFiles
    public abstract ListProperty<Directory> getLocalClassesDirs();

    /**
     * The instrumented classes destination jar file.
     *
     * @return The instrumented classes destination jar file.
     */
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

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
     * Executes the plugin for transforming all project's classes.
     */
    @TaskAction
    public void execute() {
        try {
            ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersionString(getJavaTargetCompatibilityVersion().get().toString());
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (File file : getAndroidBootClasspath().plus(getByteBuddyClasspath()).getFiles()) {
                classFileLocators.add(file.isFile()
                        ? ClassFileLocator.ForJarFile.of(file)
                        : new ClassFileLocator.ForFolder(file));
            }
            classFileLocators.add(ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader()));
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            try {
                Set<Plugin.Engine.Source> sources = new LinkedHashSet<Plugin.Engine.Source>();
                Set<File> localClasspath = new HashSet<>();
                for (Directory directory : getLocalClassesDirs().get()) {
                    File file = directory.getAsFile();
                    localClasspath.add(file);
                    sources.add(new Plugin.Engine.Source.ForFolder(file));
                }
                AndroidDescriptor androidDescriptor = DefaultAndroidDescriptor.ofClassPath(localClasspath);
                for (RegularFile jarFile : getInputJars().get()) {
                    sources.add(new Plugin.Engine.Source.ForJarFile(jarFile.getAsFile()));
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
                                .newInstance(getLogger());
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
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(AndroidDescriptor.class, androidDescriptor))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, getLogger()))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(org.slf4j.Logger.class, getLogger()))
                                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, buildLogger)));
                        } catch (Throwable throwable) {
                            throw new IllegalStateException("Cannot resolve plugin: " + name, throwable);
                        }
                    }
                    Plugin.Engine.Summary summary = Plugin.Engine.Default.of(new EntryPoint.Unvalidated(EntryPoint.Default.DECORATE),
                                    classFileVersion,
                                    MethodNameTransformer.Suffixing.withRandomSuffix())
                            .with(classFileLocator)
                            .apply(new Plugin.Engine.Source.Compound(sources), new TargetForAndroidAppJarFile(getOutputFile().get().getAsFile()), factories);
                    if (!summary.getFailed().isEmpty()) {
                        throw new IllegalStateException(summary.getFailed() + " type transformations have failed");
                    } else if (summary.getTransformed().isEmpty()) {
                        getLogger().info("No types were transformed during plugin execution");
                    } else {
                        getLogger().info("Transformed {} type(s)", summary.getTransformed().size());
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
            throw new GradleException("Failed to transform classes", exception);
        }
    }

    /**
     * A configuration action for the {@link ByteBuddyLocalClassesEnhancerTask} task.
     */
    public static class ConfigurationAction implements Action<ByteBuddyLocalClassesEnhancerTask> {

        /**
         * The current variant's Byte Buddy configuration.
         */
        private final FileCollection byteBuddyConfiguration;
        /**
         * The android gradle extension.
         */

        private final BaseExtension androidExtension;

        /**
         * @param byteBuddyConfiguration The current variant Byte Buddy configuration.
         * @param androidExtension       The android gradle extension.
         */
        public ConfigurationAction(FileCollection byteBuddyConfiguration, BaseExtension androidExtension) {
            this.byteBuddyConfiguration = byteBuddyConfiguration;
            this.androidExtension = androidExtension;
        }

        @Override
        public void execute(ByteBuddyLocalClassesEnhancerTask task) {
            task.getByteBuddyClasspath().from(byteBuddyConfiguration);
            task.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
            task.getJavaTargetCompatibilityVersion().set(androidExtension.getCompileOptions().getTargetCompatibility());
        }
    }

    /**
     * An implementation for an Android descriptor based on resolving class names against the class path.
     */
    protected static class DefaultAndroidDescriptor implements AndroidDescriptor {

        /**
         * The file name extension of a Java class file.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * The files on the class path.
         */
        private final Set<String> names;

        /**
         * Creates a default Android descriptor.
         *
         * @param names The names of all classes on the class path.
         */
        protected DefaultAndroidDescriptor(Set<String> names) {
            this.names = names;
        }

        /**
         * Resolves class names of a set of class files from the class path.
         *
         * @param roots The class path roots to resolve.
         * @return A suitable Android descriptor.
         */
        protected static AndroidDescriptor ofClassPath(Set<File> roots) {
            Set<String> names = new HashSet<String>();
            for (File root : roots) {
                Queue<File> queue = QueueFactory.make(Collections.singleton(root));
                while (!queue.isEmpty()) {
                    File file = queue.remove();
                    if (file.isDirectory()) {
                        File[] value = file.listFiles();
                        if (value != null) {
                            queue.addAll(Arrays.asList(value));
                        }
                    } else if (file.getName().endsWith(CLASS_FILE_EXTENSION)) {
                        String path = root.getAbsoluteFile().toURI().relativize(file.getAbsoluteFile().toURI()).getPath();
                        names.add(path.substring(0, path.length() - CLASS_FILE_EXTENSION.length()).replace('/', '.'));
                    }
                }
            }
            return new DefaultAndroidDescriptor(names);
        }

        /**
         * {@inheritDoc}
         */
        public TypeScope getTypeScope(TypeDescription typeDescription) {
            return names.contains(typeDescription.getName())
                    ? TypeScope.LOCAL
                    : TypeScope.EXTERNAL;
        }
    }

    /**
     * A Byte Buddy compilation target that merges an enhanced android app's runtime classpath into a jar file.
     */
    protected static class TargetForAndroidAppJarFile extends Plugin.Engine.Target.ForJarFile {

        /**
         * The targeted file.
         */
        private final File file;

        /**
         * Creates a new Byte Buddy compilation target for Android.
         *
         * @param file The targeted file.
         */
        protected TargetForAndroidAppJarFile(File file) {
            super(file);
            this.file = file;
        }

        /**
         * {@inheritDoc}
         */
        public Sink write(@MaybeNull Manifest manifest) throws IOException {
            return manifest == null
                    ? new ForAndroidAppOutputStream(new JarOutputStream(new FileOutputStream(file)))
                    : new ForAndroidAppOutputStream(new JarOutputStream(new FileOutputStream(file), manifest));
        }

        /**
         * A sink for an Android file.
         */
        protected static class ForAndroidAppOutputStream extends Sink.ForJarOutputStream {

            /**
             * The targeted output stream.
             */
            private final JarOutputStream outputStream;

            /**
             * Creates an output stream for an Android file.
             *
             * @param outputStream The targeted output stream.
             */
            protected ForAndroidAppOutputStream(JarOutputStream outputStream) {
                super(outputStream);
                this.outputStream = outputStream;
            }

            /**
             * {@inheritDoc}
             */
            public void retain(Plugin.Engine.Source.Element element) throws IOException {
                JarEntry entry = element.resolveAs(JarEntry.class);
                if (entry != null && entry.isDirectory()) {
                    return;
                }
                try {
                    outputStream.putNextEntry(new JarEntry(element.getName()));
                    InputStream inputStream = element.getInputStream();
                    try {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, length);
                        }
                    } finally {
                        inputStream.close();
                    }
                    outputStream.closeEntry();
                } catch (ZipException exception) {
                    if (!element.getName().startsWith("META-INF")) {
                        throw exception;
                    }
                }
            }
        }
    }
}
