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
package net.bytebuddy.build.gradle.android.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.android.asm.translator.UnwrappingClassVisitor;
import net.bytebuddy.build.gradle.android.asm.translator.WrappingClassVisitor;
import net.bytebuddy.build.gradle.android.utils.DefaultEntryPoint;
import net.bytebuddy.build.gradle.android.utils.Many;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.pool.TypePool;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
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

public final class BytebuddyManager {

    private static boolean initialized = false;
    private static final List<Plugin> allPlugins = new ArrayList<>();
    private static Plugin.Engine.TypeStrategy typeStrategy;
    private static ByteBuddy byteBuddy;
    private static TypePool typePool;
    private static ClassFileLocator classFileLocator;
    private static final Map<String, List<Plugin>> matchingPlugins = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, TypeDescription> matchingTypeDescription = Collections.synchronizedMap(new HashMap<>());

    public synchronized static void initialize(FileCollection runtimeClasspath,
                                               FileCollection androidBootClasspath,
                                               FileCollection byteBuddyClasspath, ListProperty<Directory> localClasses) {
        if (initialized) {
            System.out.println("Already initialized");
            return;
        }
        initialized = true;
        System.out.println("Initializing");
        EntryPoint entryPoint = new DefaultEntryPoint();
        Plugin.Engine.PoolStrategy poolStrategy = Plugin.Engine.PoolStrategy.Default.FAST;
        byteBuddy = entryPoint.byteBuddy(ClassFileVersion.JAVA_V8);//todo set version from project
        typeStrategy = new Plugin.Engine.TypeStrategy.ForEntryPoint(entryPoint, MethodNameTransformer.Suffixing.withRandomSuffix());
        try {
            Set<File> classpath = runtimeClasspath.plus(androidBootClasspath).plus(byteBuddyClasspath).getFiles();
            ArrayList<File> localClassesDirs = Many.map(localClasses.get(), Directory::getAsFile);
            classpath.addAll(localClassesDirs);
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

    public static ClassVisitor apply(String className, ClassVisitor original) {
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

    private static ClassVisitor translateToAndroids(net.bytebuddy.jar.asm.ClassVisitor composedVisitor) {
        return new UnwrappingClassVisitor(composedVisitor);
    }

    private static net.bytebuddy.jar.asm.ClassVisitor translateToByteBuddys(ClassVisitor original) {
        return new WrappingClassVisitor(original);
    }

    public static boolean matches(String className) {
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

    private static ClassFileLocator getClassFileLocator(Set<File> classpath) throws IOException {
        ArrayList<ClassFileLocator> classFileLocators = new ArrayList<>();

        for (File artifact : classpath) {
            classFileLocators.add(artifact.isFile() ? ClassFileLocator.ForJarFile.of(artifact) : new ClassFileLocator.ForFolder(artifact));
        }

        classFileLocators.add(
                ClassFileLocator.ForClassLoader.of(ByteBuddy.class.getClassLoader())
        );

        return new ClassFileLocator.Compound(classFileLocators);
    }

    private static List<Plugin.Factory> createPluginFactories(Set<File> androidBootClasspath, Set<File> bytebuddyDiscoveryClasspath) throws IOException {
        URLClassLoader androidLoader = new URLClassLoader(toUrlArray(androidBootClasspath), ByteBuddy.class.getClassLoader());
        URLClassLoader pluginLoader = new URLClassLoader(toUrlArray(bytebuddyDiscoveryClasspath), androidLoader);
        ArrayList<Plugin.Factory> factories = new ArrayList<>();

        for (String className : Plugin.Engine.Default.scan(pluginLoader)) {
            factories.add(createFactoryFromClassName(className, pluginLoader));
        }

        return factories;
    }

    private static Plugin.Factory.UsingReflection createFactoryFromClassName(
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
    private static Class<? extends Plugin> getClassFromName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> type = Class.forName(className, false, classLoader);

        if (!Plugin.class.isAssignableFrom(type)) {
            throw new GradleException(type.getName() + " does not implement " + Plugin.class.getName());
        }

        return (Class<? extends Plugin>) type;
    }

    private static URL[] toUrlArray(Set<File> files) {
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
}