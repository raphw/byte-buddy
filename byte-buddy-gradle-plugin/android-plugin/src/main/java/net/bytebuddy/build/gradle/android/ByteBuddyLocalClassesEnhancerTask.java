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
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.QueueFactory;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.*;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

/**
 * Transformation task for instrumenting the project's local and dependencies' classes.
 */
public abstract class ByteBuddyLocalClassesEnhancerTask extends DefaultTask {

    @Nested
    public abstract ListProperty<Transformation> getTransformations();

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
     * Determines the discovery for finding plugins on the class path.
     *
     * @return The discovery for finding plugins on the class path.
     */
    @Input
    public abstract Property<Discovery> getDiscovery();

    /**
     * Returns the entry point to use for instrumentations. If not set, the instrumented classes
     * will be rebased without type validation.
     *
     * @return The entry point to use for instrumentations.
     */
    @Input
    public abstract Property<EntryPoint> getEntryPoint();

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
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @return {@code true} if a warning should be issued for an empty type set.
     */
    @Internal
    public abstract Property<Boolean> getWarnOnEmptyTypeSet();

    /**
     * Returns {@code true} if this task should fail fast.
     *
     * @return {@code true} if this task should fail fast.
     */
    @Internal
    public abstract Property<Boolean> getFailFast();

    /**
     * Returns {@code true} if the transformation should fail if a live initializer is used.
     *
     * @return {@code true} if the transformation should fail if a live initializer is used.
     */
    @Internal
    public abstract Property<Boolean> getFailOnLiveInitializer();

    /**
     * Returns the number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     *
     * @return The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    @Internal
    public abstract Property<Integer> getThreads();

    /**
     * Returns the suffix to use for rebased methods or the empty string if a random suffix should be used.
     *
     * @return The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    @Input
    public abstract Property<String> getSuffix();

    /**
     * Returns {@code true} if extended parsing should be used.
     *
     * @return {@code true} if extended parsing should be used.
     */
    @Input
    public abstract Property<Boolean> getExtendedParsing();

    /**
     * Returns the source set to resolve plugin names from or {@code null} if no such source set is used.
     *
     * @return The source set to resolve plugin names from or {@code null} if no such source set is used.
     */
    @MaybeNull
    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getDiscoverySet();

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
     *
     * @throws IOException If an I/O exception occurs.
     */
    @TaskAction
    public void execute() throws IOException {
        List<Object> transformations = new ArrayList<Object>(getTransformations().get().size());
        for (Transformation transformation : getTransformations().get()) {
            transformations.add(transformation.resolve());
        }
        Set<Plugin.Engine.Source> sources = new LinkedHashSet<Plugin.Engine.Source>();
        Set<File> localClasspath = new HashSet<>();
        for (Directory directory : getLocalClassesDirs().get()) {
            File file = directory.getAsFile();
            localClasspath.add(file);
            sources.add(new Plugin.Engine.Source.ForFolder(file));
        }
        for (RegularFile jarFile : getInputJars().get()) {
            sources.add(new Plugin.Engine.Source.ForJarFile(jarFile.getAsFile()));
        }
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersionString(getJavaTargetCompatibilityVersion().get().toString());
        AndroidDescriptor androidDescriptor = DefaultAndroidDescriptor.ofClassPath(localClasspath);
        ClassLoader classLoader = new URLClassLoader(
                toUrls(getByteBuddyClasspath().getFiles()),
                new URLClassLoader(toUrls(getAndroidBootClasspath().getFiles()), ByteBuddy.class.getClassLoader()));
        try {
            Class<?> discovery = Class.forName("net.bytebuddy.build.gradle.Discovery");
            Class.forName("net.bytebuddy.build.gradle.AbstractByteBuddyTask").getMethod("apply",
                Logger.class,
                ClassLoader.class,
                List.class,
                discovery,
                ClassFileLocator.class,
                Iterable.class,
                Iterable.class,
                EntryPoint.class,
                ClassFileVersion.class,
                ClassFileVersion.class,
                Plugin.Factory.UsingReflection.ArgumentResolver.class,
                String.class,
                int.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                Plugin.Engine.Source.class,
                Plugin.Engine.Target.class).invoke(null,
                    getLogger(),
                    classLoader,
                    transformations,
                    discovery.getMethod("valueOf", String.class).invoke(null, getDiscovery().get().name()),
                    ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader()),
                    getAndroidBootClasspath().plus(getByteBuddyClasspath()).getFiles(),
                    getDiscoverySet().getFiles(),
                    getEntryPoint().get(),
                    classFileVersion,
                    classFileVersion,
                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(AndroidDescriptor.class, androidDescriptor),
                    getSuffix().get(),
                    getThreads().get(),
                    getExtendedParsing().get(),
                    getFailFast().get(),
                    getFailOnLiveInitializer().get(),
                    getWarnOnEmptyTypeSet().get(),
                    new Plugin.Engine.Source.Compound(sources),
                    new TargetForAndroidAppJarFile(getOutputFile().get().getAsFile()));
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof RuntimeException){
                throw (RuntimeException) cause;
            } else {
                throw new GradleException("Unexpected transformation error", cause);
            }
        } catch (Throwable throwable) {
            throw new GradleException("Unexpected transformation error", throwable);
        } finally {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            }
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
         * The Android gradle extension.
         */
        private final BaseExtension androidExtension;

        /**
         * The Byte Buddy task extension.
         */
        private final ByteBuddyAndroidTaskExtension byteBuddyExtension;

        /**
         * @param byteBuddyConfiguration The current variant Byte Buddy configuration.
         * @param androidExtension       The Android gradle extension.
         * @param byteBuddyExtension     The Byte Buddy task extension.
         */
        public ConfigurationAction(FileCollection byteBuddyConfiguration,
                                   BaseExtension androidExtension,
                                   ByteBuddyAndroidTaskExtension byteBuddyExtension) {
            this.byteBuddyConfiguration = byteBuddyConfiguration;
            this.androidExtension = androidExtension;
            this.byteBuddyExtension = byteBuddyExtension;
        }

        @Override
        public void execute(ByteBuddyLocalClassesEnhancerTask task) {
            task.getByteBuddyClasspath().from(byteBuddyConfiguration);
            task.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
            task.getJavaTargetCompatibilityVersion().set(androidExtension.getCompileOptions().getTargetCompatibility());
            byteBuddyExtension.configure(task);
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
            OutputStream outputStream = new FileOutputStream(file);
            try {
                return manifest == null
                        ? new ForAndroidAppOutputStream(new JarOutputStream(outputStream))
                        : new ForAndroidAppOutputStream(new JarOutputStream(outputStream, manifest));
            } catch (IOException exception) {
                outputStream.close();
                throw exception;
            } catch (RuntimeException exception) {
                outputStream.close();
                throw exception;
            } catch (Error error) {
                outputStream.close();
                throw error;
            }
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
                String name = element.getName();
                try {
                    outputStream.putNextEntry(new JarEntry(name));
                    if (!name.endsWith("/")) {
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
                    }
                    outputStream.closeEntry();
                } catch (ZipException exception) {
                    if (!name.startsWith("META-INF") && !name.endsWith("-info.class") && name.endsWith(".class")) {
                        throw exception;
                    }
                }
            }
        }
    }
}
