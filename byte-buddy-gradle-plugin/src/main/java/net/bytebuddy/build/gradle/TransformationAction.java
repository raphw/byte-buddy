package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.pool.TypePool;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformationAction implements Action<Task> {

    private static final String CLASS_FILE_EXTENSION = ".class";

    private final Project project;

    private final ByteBuddyExtension byteBuddyExtension;

    private final AbstractCompile compileTask;

    public TransformationAction(Project project, ByteBuddyExtension extension, AbstractCompile compileTask) {
        this.project = project;
        this.byteBuddyExtension = extension;
        this.compileTask = compileTask;
    }

    @Override
    public void execute(Task task) {
        try {
            processOutputDirectory(compileTask.getDestinationDir(), compileTask.getClasspath());
        } catch (IOException exception) {
            throw new GradleException("Error accessing file system", exception);
        }
    }

    private void processOutputDirectory(File root, Iterable<? extends File> classPath) throws IOException {
        if (!root.isDirectory()) {
            throw new GradleException("Target location does not exist or is no directory: " + root);
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver();
        try {
            List<Plugin> plugins = new ArrayList<Plugin>(byteBuddyExtension.getTransformations().size());
            for (Transformation transformation : byteBuddyExtension.getTransformations()) {
                try {
                    String plugin = transformation.getPlugin();
                    plugins.add((Plugin) Class.forName(plugin, false, classLoaderResolver.resolve(transformation.getClassPath(root, classPath)))
                            .getDeclaredConstructor()
                            .newInstance());
                    project.getLogger().info("Created plugin: {}", plugin);
                } catch (Exception exception) {
                    throw new GradleException("Cannot create plugin: " + transformation.getRawPlugin(), exception);
                }
            }
            EntryPoint entryPoint = byteBuddyExtension.getInitialization().toEntryPoint(classLoaderResolver, root, classPath);
            project.getLogger().info("Resolved entry point: {}", entryPoint);
            transform(root, classPath, entryPoint, plugins);
        } finally {
            classLoaderResolver.close();
        }
    }

    private void transform(File root, Iterable<? extends File> classPath, EntryPoint entryPoint, List<Plugin> plugins) throws IOException {
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
        classFileLocators.add(new ClassFileLocator.ForFolder(root));
        for (File artifact : classPath) {
            classFileLocators.add(artifact.isFile()
                    ? ClassFileLocator.ForJarFile.of(artifact)
                    : new ClassFileLocator.ForFolder(artifact));
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
        try {
            TypePool typePool = new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                    classFileLocator,
                    TypePool.Default.ReaderMode.FAST,
                    TypePool.ClassLoading.ofBootPath());
            project.getLogger().info("Processing class files located in in: {}", root);
            ByteBuddy byteBuddy;
            try {
                byteBuddy = entryPoint.getByteBuddy();
            } catch (Throwable throwable) {
                throw new GradleException("Cannot create Byte Buddy instance", throwable);
            }
            processDirectory(root,
                    root,
                    byteBuddy,
                    entryPoint,
                    byteBuddyExtension.getSuffix() == null || byteBuddyExtension.getSuffix().isEmpty()
                            ? MethodNameTransformer.Suffixing.withRandomSuffix()
                            : new MethodNameTransformer.Suffixing(byteBuddyExtension.getSuffix()),
                    classFileLocator,
                    typePool,
                    plugins);
        } finally {
            classFileLocator.close();
        }
    }

    private void processDirectory(File root,
                                  File folder,
                                  ByteBuddy byteBuddy,
                                  EntryPoint entryPoint,
                                  MethodNameTransformer methodNameTransformer,
                                  ClassFileLocator classFileLocator,
                                  TypePool typePool,
                                  List<Plugin> plugins) {
        File[] file = folder.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isDirectory()) {
                    processDirectory(root, aFile, byteBuddy, entryPoint, methodNameTransformer, classFileLocator, typePool, plugins);
                } else if (aFile.isFile() && aFile.getName().endsWith(CLASS_FILE_EXTENSION)) {
                    processClassFile(root,
                            root.toURI().relativize(aFile.toURI()).toString(),
                            byteBuddy,
                            entryPoint,
                            methodNameTransformer,
                            classFileLocator,
                            typePool,
                            plugins);
                } else {
                    project.getLogger().debug("Skipping ignored file: {}", aFile);
                }
            }
        }
    }

    private void processClassFile(File root,
                                  String file,
                                  ByteBuddy byteBuddy,
                                  EntryPoint entryPoint,
                                  MethodNameTransformer methodNameTransformer,
                                  ClassFileLocator classFileLocator,
                                  TypePool typePool,
                                  List<Plugin> plugins) {
        String typeName = file.replace('/', '.').substring(0, file.length() - CLASS_FILE_EXTENSION.length());
        project.getLogger().debug("Processing class file: {}", typeName);
        TypeDescription typeDescription = typePool.describe(typeName).resolve();
        DynamicType.Builder<?> builder;
        try {
            builder = entryPoint.transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
        } catch (Throwable throwable) {
            throw new GradleException("Cannot transform type: " + typeName, throwable);
        }
        boolean transformed = false;
        for (Plugin plugin : plugins) {
            try {
                if (plugin.matches(typeDescription)) {
                    builder = plugin.apply(builder, typeDescription);
                    transformed = true;
                }
            } catch (Throwable throwable) {
                throw new GradleException("Cannot apply " + plugin + " on " + typeName, throwable);
            }
        }
        if (transformed) {
            project.getLogger().info("Transformed type: {}", typeName);
            DynamicType dynamicType = builder.make();
            for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : dynamicType.getLoadedTypeInitializers().entrySet()) {
                if (byteBuddyExtension.isFailOnLiveInitializer() && entry.getValue().isAlive()) {
                    throw new GradleException("Cannot apply live initializer for " + entry.getKey());
                }
            }
            try {
                dynamicType.saveIn(root);
            } catch (IOException exception) {
                throw new GradleException("Cannot save " + typeName + " in " + root, exception);
            }
        } else {
            project.getLogger().debug("Skipping non-transformed type: {}", typeName);
        }
    }
}
