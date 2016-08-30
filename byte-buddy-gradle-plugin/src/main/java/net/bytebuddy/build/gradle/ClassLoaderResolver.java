package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ClassLoaderResolver implements Closeable {

    private final Logger logger;

    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession repositorySystemSession;

    private final List<RemoteRepository> remoteRepositories;

    private final Map<MavenCoordinate, ClassLoader> classLoaders;

    public ClassLoaderResolver(Project project) throws MalformedURLException {
        this.logger = project.getLogger();
        repositorySystem = MavenRepositorySystemUtils.newServiceLocator()
                .setService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                .setService(TransporterFactory.class, WagonTransporterFactory.class)
                .setService(WagonProvider.class, ManualWagonProvider.class)
                .getService(RepositorySystem.class);
        repositorySystemSession = MavenRepositorySystemUtils.newSession();
        repositorySystem.newLocalRepositoryManager(repositorySystemSession, new LocalRepository(project.getRepositories().mavenLocal().getUrl().toString()));
        // TODO: Extract from repository list!
        remoteRepositories = Collections.singletonList(new RemoteRepository.Builder("maven-central", "default", project.getRepositories().mavenCentral().getUrl().toString()).build());
        classLoaders = new HashMap<MavenCoordinate, ClassLoader>();
    }

    public ClassLoader resolve(MavenCoordinate mavenCoordinate) {
        ClassLoader classLoader = classLoaders.get(mavenCoordinate);
        if (classLoader == null) {
            classLoader = doResolve(mavenCoordinate);
            classLoaders.put(mavenCoordinate, classLoader);
        }
        return classLoader;
    }

    protected ClassLoader doResolve(MavenCoordinate mavenCoordinate) {
        List<URL> urls = new ArrayList<URL>();
        logger.info("Resolving transformer dependency: {}", mavenCoordinate);
        try {
            DependencyNode root = repositorySystem.collectDependencies(repositorySystemSession, new CollectRequest(new Dependency(mavenCoordinate.asArtifact(), "runtime"), remoteRepositories)).getRoot();
            repositorySystem.resolveDependencies(repositorySystemSession, new DependencyRequest().setRoot(root));
            PreorderNodeListGenerator preorderNodeListGenerator = new PreorderNodeListGenerator();
            root.accept(preorderNodeListGenerator);
            for (Artifact artifact : preorderNodeListGenerator.getArtifacts(false)) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (DependencyCollectionException exception) {
            throw new GradleException("Could not collect dependencies for " + mavenCoordinate, exception);
        } catch (DependencyResolutionException exception) {
            throw new GradleException("Could not resolve dependencies for " + mavenCoordinate, exception);
        } catch (MalformedURLException exception) {
            throw new GradleException("Could not resolve file as URL for " + mavenCoordinate, exception);
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

    public static class ManualWagonProvider implements WagonProvider {

        private static final Wagon NO_WAGON = null;

        @Override
        public Wagon lookup(String roleHint) throws Exception {
            return "http".equals(roleHint) || "https".equals(roleHint)
                    ? new HttpWagon()
                    : NO_WAGON;
        }

        @Override
        public void release(Wagon wagon) {
            if (wagon instanceof HttpWagon) {
                ((HttpWagon) wagon).closeConnection();
            }
        }
    }
}
