package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import org.gradle.api.GradleException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class loader resolver for creating class loaders for given class paths.
 */
public class ClassLoaderResolver implements Closeable {

    /**
     * A mapping of files collections to previously created class loaders.
     */
    private final Map<Set<? extends File>, ClassLoader> classLoaders;

    /**
     * Creates a new class loader resolver.
     */
    public ClassLoaderResolver() {
        classLoaders = new HashMap<Set<? extends File>, ClassLoader>();
    }

    /**
     * Resolves a class path to a class loader. If a class loader for the same file collection was created
     * previously, the previous class loader is returned.
     *
     * @param classPath The class path to consider.
     * @return A class loader for the supplied class path.
     */
    public ClassLoader resolve(Iterable<? extends File> classPath) {
        Set<File> classPathList = new LinkedHashSet<File>();
        for (File file : classPath) {
            classPathList.add(file);
        }
        return resolve(classPathList);
    }

    /**
     * Resolves a class path to a class loader. If a class loader for the same file collection was created
     * previously, the previous class loader is returned.
     *
     * @param classPath The class path to consider.
     * @return A class loader for the supplied class path.
     */
    private ClassLoader resolve(Set<? extends File> classPath) {
        ClassLoader classLoader = classLoaders.get(classPath);
        if (classLoader == null) {
            classLoader = doResolve(classPath);
            classLoaders.put(classPath, classLoader);
        }
        return classLoader;
    }

    /**
     * Resolves a class path to a class loader.
     *
     * @param classPath The class path to consider.
     * @return A class loader for the supplied class path.
     */
    private ClassLoader doResolve(Set<? extends File> classPath) {
        List<URL> urls = new ArrayList<URL>(classPath.size());
        for (File file : classPath) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException exception) {
                throw new GradleException("Cannot resolve " + file + " as URL", exception);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), ByteBuddy.class.getClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        for (ClassLoader classLoader : classLoaders.values()) {
            if (classLoader instanceof Closeable) { // URLClassLoaders are only closeable since Java 1.7.
                ((Closeable) classLoader).close();
            }
        }
    }
}
