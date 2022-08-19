package net.bytebuddy.build.gradle.android.service;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.android.asm.translator.UnwrappingClassVisitor;
import net.bytebuddy.build.gradle.android.asm.translator.WrappingClassVisitor;
import net.bytebuddy.build.gradle.android.utils.DefaultEntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.pool.TypePool;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class BytebuddyService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    private boolean initialized = false;
    private final List<Plugin> allPlugins = new ArrayList<>();
    private Plugin.Engine.TypeStrategy typeStrategy;
    private ByteBuddy byteBuddy;
    private TypePool typePool;
    private ClassFileLocator classFileLocator;
    private final Map<String, List<Plugin>> matchingPlugins = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, TypeDescription> matchingTypeDescription = Collections.synchronizedMap(new HashMap<>());

    public synchronized void initialize(FileCollection runtimeClasspath,
                                        FileCollection androidBootClasspath,
                                        FileCollection byteBuddyClasspath,
                                        FileCollection localClasses) {
        if (initialized) {
            System.out.println("Already initialized service");
            return;
        }
        initialized = true;
        System.out.println("Initializing");
        EntryPoint entryPoint = new DefaultEntryPoint();
        Plugin.Engine.PoolStrategy poolStrategy = Plugin.Engine.PoolStrategy.Default.FAST;
        byteBuddy = entryPoint.byteBuddy(ClassFileVersion.JAVA_V8);//todo set version from project
        typeStrategy = new Plugin.Engine.TypeStrategy.ForEntryPoint(entryPoint, MethodNameTransformer.Suffixing.withRandomSuffix());
        try {
            Set<File> classpath = runtimeClasspath.plus(androidBootClasspath).plus(byteBuddyClasspath).plus(localClasses).getFiles();
            classFileLocator = getClassFileLocator(classpath);
            typePool = poolStrategy.typePool(classFileLocator);
            List<? extends Plugin.Factory> factories = createPluginFactories(androidBootClasspath.getFiles(), byteBuddyClasspath.getFiles());
            for (Plugin.Factory factory : factories) {
                allPlugins.add(factory.make());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished initializing");
    }


    public ClassVisitor apply(String className, ClassVisitor original) {
        TypeDescription typeDescription = matchingTypeDescription.get(className);
        List<Plugin> classMatchingPlugins = matchingPlugins.get(className);
        matchingTypeDescription.remove(className);
        matchingPlugins.remove(className);

        net.bytebuddy.jar.asm.ClassVisitor composedVisitor = translateToByteBuddys(original);
        DynamicType.Builder<?> builder = typeStrategy.builder(byteBuddy, typeDescription, classFileLocator);
        for (Plugin matchingPlugin : classMatchingPlugins) {
            composedVisitor = matchingPlugin.apply(builder, typeDescription, classFileLocator).wrap(composedVisitor);
        }
        return translateToAndroids(composedVisitor);
    }

    private ClassVisitor translateToAndroids(net.bytebuddy.jar.asm.ClassVisitor composedVisitor) {
        return new UnwrappingClassVisitor(composedVisitor);
    }

    private net.bytebuddy.jar.asm.ClassVisitor translateToByteBuddys(ClassVisitor original) {
        return new WrappingClassVisitor(original);
    }

    public boolean matches(String className) {
        List<Plugin> plugins = new ArrayList<>();
        TypeDescription typeDescription = typePool.describe(className).resolve();
        for (Plugin plugin : allPlugins) {
            if (plugin.matches(typeDescription)) {
                plugins.add(plugin);
            }
        }

        boolean matches = !plugins.isEmpty();

        if (matches) {
            matchingTypeDescription.put(className, typeDescription);
            matchingPlugins.put(className, plugins);
        }

        return matches;
    }

    private ClassFileLocator getClassFileLocator(Set<File> classpath) throws IOException {
        ArrayList<ClassFileLocator> classFileLocators = new ArrayList<>();

        for (File artifact : classpath) {
            classFileLocators.add(artifact.isFile() ? ClassFileLocator.ForJarFile.of(artifact) : new ClassFileLocator.ForFolder(artifact));
        }

        classFileLocators.add(
                ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader())
        );

        return new ClassFileLocator.Compound(classFileLocators);
    }

    private List<Plugin.Factory> createPluginFactories(Set<File> androidBootClasspath, Set<File> bytebuddyDiscoveryClasspath) throws IOException {
        URLClassLoader androidLoader = new URLClassLoader(toUrlArray(androidBootClasspath), ByteBuddy.class.getClassLoader());
        URLClassLoader pluginLoader = new URLClassLoader(toUrlArray(bytebuddyDiscoveryClasspath), androidLoader);
        ArrayList<Plugin.Factory> factories = new ArrayList<>();

        for (String className : Plugin.Engine.Default.scan(pluginLoader)) {
            factories.add(createFactoryFromClassName(className, pluginLoader));
        }

        return factories;
    }

    private Plugin.Factory.UsingReflection createFactoryFromClassName(
            String className,
            ClassLoader classLoader
    ) {
        try {
            Class<? extends Plugin> pluginClass = getClassFromName(className, classLoader);
            return new Plugin.Factory.UsingReflection(pluginClass);
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot resolve plugin: " + className, t);
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

    @Override
    public void close() throws Exception {
        System.out.println("Closing service");
    }
}
