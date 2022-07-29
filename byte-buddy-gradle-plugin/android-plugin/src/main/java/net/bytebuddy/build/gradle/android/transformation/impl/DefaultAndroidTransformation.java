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
package net.bytebuddy.build.gradle.android.transformation.impl;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import net.bytebuddy.build.gradle.android.transformation.impl.source.CompoundSourceOrigin;
import net.bytebuddy.build.gradle.common.TransformationLogger;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.gradle.api.GradleException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class DefaultAndroidTransformation implements AndroidTransformation {
    private final Logger logger;

    public DefaultAndroidTransformation(Logger logger) {
        this.logger = logger;
    }

    public void transform(AndroidTransformation.Input input, File outputDir) {
        CompoundSourceOrigin source = createSource(input.targetClasspath);

        Set<File> contextClasspath = new HashSet<>();
        contextClasspath.addAll(input.referenceClasspath);
        contextClasspath.addAll(input.androidBootClasspath);
        contextClasspath.addAll(input.bytebuddyDiscoveryClasspath);

        try (ClassFileLocator contextClassFileLocator = createContextClassFileLocator(contextClasspath)) {
            makeEngine(input.jvmTargetVersion)
                    .with(contextClassFileLocator)
                    .apply(
                            source,
                            new Plugin.Engine.Target.ForFolder(outputDir),
                            createPluginFactories(input)
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Plugin.Engine makeEngine(int javaTargetCompatibilityVersion) {
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersion(javaTargetCompatibilityVersion);
        MethodNameTransformer methodNameTransformer = MethodNameTransformer.Suffixing.withRandomSuffix();

        return Plugin.Engine.Default.of(new DefaultEntryPoint(), classFileVersion, methodNameTransformer)
                .with(new TransformationLogger(logger));
    }

    private List<Plugin.Factory> createPluginFactories(AndroidTransformation.Input input) throws IOException {
        URLClassLoader androidLoader = new URLClassLoader(toUrlArray(input.androidBootClasspath), ByteBuddy.class.getClassLoader());
        URLClassLoader pluginLoader = new URLClassLoader(toUrlArray(input.bytebuddyDiscoveryClasspath), androidLoader);
        ArrayList<Plugin.Factory> factories = new ArrayList<>();

        for (String className : Plugin.Engine.Default.scan(pluginLoader)) {
            factories.add(createFactoryFromClassName(className, pluginLoader));
            logger.info("Resolved plugin: {}", className);
        }

        return factories;
    }

    private Plugin.Factory.UsingReflection createFactoryFromClassName(
            String className,
            ClassLoader classLoader
    ) {
        try {
            Class<? extends Plugin> pluginClass = getClassFromName(className, classLoader);
            return new Plugin.Factory.UsingReflection(pluginClass)
                    .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, logger));
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

    private CompoundSourceOrigin createSource(Set<File> targetClasspath) {
        Set<Plugin.Engine.Source.Origin> origins = getOriginsFromClasspath(targetClasspath);
        return new CompoundSourceOrigin(origins);
    }

    private Set<Plugin.Engine.Source.Origin> getOriginsFromClasspath(
            Set<File> targetClasspath
    ) {
        Set<Plugin.Engine.Source.Origin> origins = new HashSet<>();

        targetClasspath.forEach(it -> {
            Plugin.Engine.Source.Origin origin;
            if (it.isDirectory()) {
                origin = new Plugin.Engine.Source.ForFolder(it);
            } else {
                try {
                    origin = new Plugin.Engine.Source.Origin.ForJarFile(new JarFile(it));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            origins.add(origin);
        });

        return origins;
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
}