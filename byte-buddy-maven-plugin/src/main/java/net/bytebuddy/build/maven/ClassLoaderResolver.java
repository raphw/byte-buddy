/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.build.maven;

import net.bytebuddy.ByteBuddy;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A resolver that transforms a Maven coordinate into a class loader that can view the dependencies implied by this coordinate.
 */
public class ClassLoaderResolver implements Closeable {

    /**
     * The Maven log dispatcher.
     */
    private final Log log;

    /**
     * The repository system to use.
     */
    private final RepositorySystem repositorySystem;

    /**
     * The repository system session to use.
     */
    private final RepositorySystemSession repositorySystemSession;

    /**
     * A list of remote repositories available.
     */
    private final List<RemoteRepository> remoteRepositories;

    /**
     * A mapping of Maven coordinates to already existing class loaders.
     */
    private final Map<MavenCoordinate, ClassLoader> classLoaders;

    /**
     * Creates a new class loader resolver.
     *
     * @param log                     The Maven log dispatcher.
     * @param repositorySystem        The repository system to use.
     * @param repositorySystemSession The repository system session to use.
     * @param remoteRepositories      A list of remote repositories available.
     */
    public ClassLoaderResolver(Log log, RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) {
        this.log = log;
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteRepositories = remoteRepositories;
        classLoaders = new HashMap<MavenCoordinate, ClassLoader>();
    }


    /**
     * Resolves a Maven coordinate to a class loader that can load all of the coordinates classes. If a Maven coordinate was resolved previously,
     * the previously created class loader is returned.
     *
     * @param mavenCoordinate The Maven coordinate to resolve.
     * @return A class loader that references all of the class loader's dependencies and which is a child of this class's class loader.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     */
    public ClassLoader resolve(MavenCoordinate mavenCoordinate) throws MojoFailureException, MojoExecutionException {
        ClassLoader classLoader = classLoaders.get(mavenCoordinate);
        if (classLoader == null) {
            classLoader = doResolve(mavenCoordinate);
            classLoaders.put(mavenCoordinate, classLoader);
        }
        return classLoader;
    }

    /**
     * Resolves a Maven coordinate to a class loader that can load all of the coordinates classes.
     *
     * @param mavenCoordinate The Maven coordinate to resolve.
     * @return A class loader that references all of the class loader's dependencies and which is a child of this class's class loader.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     */
    private ClassLoader doResolve(MavenCoordinate mavenCoordinate) throws MojoExecutionException, MojoFailureException {
        List<URL> urls = new ArrayList<URL>();
        log.info("Resolving transformer dependency: " + mavenCoordinate);
        try {
            DependencyNode root = repositorySystem.collectDependencies(repositorySystemSession, new CollectRequest(new Dependency(mavenCoordinate.asArtifact(), "runtime"), remoteRepositories)).getRoot();
            repositorySystem.resolveDependencies(repositorySystemSession, new DependencyRequest().setRoot(root));
            PreorderNodeListGenerator preorderNodeListGenerator = new PreorderNodeListGenerator();
            root.accept(preorderNodeListGenerator);
            for (Artifact artifact : preorderNodeListGenerator.getArtifacts(false)) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (DependencyCollectionException exception) {
            throw new MojoExecutionException("Could not collect dependencies for " + mavenCoordinate, exception);
        } catch (DependencyResolutionException exception) {
            throw new MojoExecutionException("Could not resolve dependencies for " + mavenCoordinate, exception);
        } catch (MalformedURLException exception) {
            throw new MojoFailureException("Could not resolve file as URL for " + mavenCoordinate, exception);
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
