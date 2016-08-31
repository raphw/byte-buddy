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
import java.util.List;
import java.util.Map;

public class ClassLoaderResolver implements Closeable {

    private final Map<List<File>, ClassLoader> classLoaders;

    public ClassLoaderResolver() throws MalformedURLException {
        this.classLoaders = new HashMap<List<File>, ClassLoader>();
    }

    public ClassLoader resolve(Iterable<? extends File> classPath) {
        List<File> classPathList = new ArrayList<File>();
        for (File file : classPath) {
            classPathList.add(file);
        }
        return resolve(classPathList);
    }

    protected ClassLoader resolve(List<File> classPath) {
        ClassLoader classLoader = classLoaders.get(classPath);
        if (classLoader == null) {
            classLoader = doResolve(classPath);
            classLoaders.put(classPath, classLoader);
        }
        return classLoader;
    }

    private ClassLoader doResolve(List<File> classPath) {
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

    @Override
    public void close() throws IOException {
        for (ClassLoader classLoader : classLoaders.values()) {
            if (classLoader instanceof Closeable) { // URLClassLoaders are only closeable since Java 1.7.
                ((Closeable) classLoader).close();
            }
        }
    }
}
