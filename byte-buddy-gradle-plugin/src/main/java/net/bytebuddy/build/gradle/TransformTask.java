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
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TransformTask extends DefaultTask {

    private static final String CLASS_FILE_EXTENSION = ".class";

    private final ByteBuddyExtension byteBuddyExtension;

    public TransformTask(String description) {
        setGroup("byte-buddy");
        setDescription(description);
        // TODO: Add run dependency, add auto life-cycle?
        byteBuddyExtension = getProject().getExtensions().getByType(ByteBuddyExtension.class);
    }

    @TaskAction
    public void doExecute() {
        try {
            processOutputDirectory(resolve(new File(getProject().getBuildDir(), "classes")), null); // TODO: Dependencies
        } catch (IOException exception) {
            throw new GradleException("Error during writing process", exception);
        }
    }

    protected abstract File resolve(File target);

    private void processOutputDirectory(File root, List<String> classPath) throws IOException {
        if (!root.isDirectory()) {
            throw new GradleException("Target location does not exist or is no directory: " + root);
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver(); // TODO: Expose repositories.
        try {
            List<Plugin> plugins = new ArrayList<Plugin>(byteBuddyExtension.getTransformations().size());
            for (Transformation transformation : byteBuddyExtension.getTransformations()) {
                try {
                    String plugin = transformation.getPlugin();
                    plugins.add((Plugin) Class.forName(plugin, false, classLoaderResolver.resolve(transformation))
                            .getDeclaredConstructor()
                            .newInstance());
                    getLogger().info("Created plugin: {}", plugin);
                } catch (Exception exception) {
                    throw new GradleException("Cannot create plugin: " + transformation, exception);
                }
            }
            EntryPoint entryPoint = byteBuddyExtension.getInitialization().toEntryPoint(classLoaderResolver);
            getLogger().info("Resolved entry point: {}", entryPoint);
            transform(root, entryPoint, classPath, plugins);
        } finally {
            classLoaderResolver.close();
        }
    }

    private void transform(File root, EntryPoint entryPoint, List<? extends String> classPath, List<Plugin> plugins) throws IOException {
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(classPath.size() + 1);
        classFileLocators.add(new ClassFileLocator.ForFolder(root));
        for (String target : classPath) {
            File artifact = new File(target);
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
            getLogger().info("Processing class files located in in: {}", root);
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
                    getLogger().debug("Skipping ignored file: {}", aFile);
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
        getLogger().debug("Processing class file: {}", typeName);
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
            getLogger().info("Transformed type: {}", typeName);
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
            getLogger().debug("Skipping non-transformed type: {}", typeName);
        }
    }

    public static class ForProductionTypes extends TransformTask {

        public ForProductionTypes() {
            super("Applies all registered Byte Buddy plugins to this project's production code");
        }

        @Override
        protected File resolve(File target) {
            return new File(target, "main");
        }
    }

    public static class ForTestTypes extends TransformTask {

        public ForTestTypes() {
            super("Applies all registered Byte Buddy plugins to this project's test code");
        }

        @Override
        protected File resolve(File target) {
            return new File(target, "test");
        }
    }
}
