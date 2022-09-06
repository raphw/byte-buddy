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

import com.android.build.api.variant.AndroidComponentsExtension;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorFactory;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.pool.TypePool;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceSpec;
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

abstract public class ByteBuddyService implements BuildService<ByteBuddyService.Params>, AutoCloseable {

    private boolean initialized = false;
    private List<Plugin> allPlugins;
    private Plugin.Engine.TypeStrategy typeStrategy;
    private ByteBuddy byteBuddy;
    private TypePool typePool;
    private ClassFileLocator classFileLocator;
    private Map<String, List<Plugin>> matchingPlugins;
    private Map<String, TypeDescription> matchingTypeDescription;
    private URLClassLoader pluginLoader;
    private ClassVisitorFactory<ClassVisitor> classVisitorFactory;

    public interface Params extends BuildServiceParameters {
        Property<JavaVersion> getJavaTargetCompatibilityVersion();
    }

    public synchronized void initialize(FileCollection runtimeClasspath,
                                        FileCollection androidBootClasspath,
                                        FileCollection byteBuddyClasspath,
                                        FileCollection localClasses) {
        if (initialized) {
            return;
        }
        initialized = true;
        allPlugins = new ArrayList<>();
        matchingPlugins = Collections.synchronizedMap(new HashMap<>());
        matchingTypeDescription = Collections.synchronizedMap(new HashMap<>());
        EntryPoint entryPoint = new EntryPoint.Unvalidated(EntryPoint.Default.REBASE);
        Plugin.Engine.PoolStrategy poolStrategy = Plugin.Engine.PoolStrategy.Default.FAST;
        ClassFileVersion version = ClassFileVersion.ofJavaVersionString(getParameters().getJavaTargetCompatibilityVersion().get().toString());
        byteBuddy = entryPoint.byteBuddy(version);
        typeStrategy = new Plugin.Engine.TypeStrategy.ForEntryPoint(entryPoint, MethodNameTransformer.Suffixing.withRandomSuffix());
        classVisitorFactory = ClassVisitorFactory.of(ClassVisitor.class, byteBuddy);
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
        return classVisitorFactory.wrap(composedVisitor);
    }

    private net.bytebuddy.jar.asm.ClassVisitor translateToByteBuddys(ClassVisitor original) {
        return classVisitorFactory.unwrap(original);
    }

    public boolean matches(String className) {
        try {
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
        } catch (TypePool.Resolution.NoSuchTypeException e) {
            // There are android generated classes for android XML resources, that typically end with "R$[something]",
            // such as "R$layout, R$string, R$id", etc. Which are not available in the classpath by the time this
            // task runs, and also, those classes aren't worthy of instrumentation either.
            return false;
        }
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
        pluginLoader = new URLClassLoader(toUrlArray(bytebuddyDiscoveryClasspath), androidLoader);
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
        for (Plugin plugin : allPlugins) {
            plugin.close();
        }
        allPlugins = null;
        matchingPlugins = null;
        initialized = false;
        pluginLoader.close();
    }

    protected static class ConfigurationAction implements Action<BuildServiceSpec<ByteBuddyService.Params>> {

        private final AndroidComponentsExtension<?, ?, ?> extension;

        protected ConfigurationAction(AndroidComponentsExtension<?, ?, ?> extension) {
            this.extension = extension;
        }

        @Override
        public void execute(BuildServiceSpec<ByteBuddyService.Params> spec) {
            spec.getParameters()
                    .getJavaTargetCompatibilityVersion()
                    .set(extension.getCompileOptions().getTargetCompatibility());
        }
    }
}
