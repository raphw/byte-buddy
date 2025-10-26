package net.bytebuddy.build.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassLoaderResolverTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", JAR = "jar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Log log;

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession repositorySystemSession;

    @Mock
    private DependencyNode root, child;

    private ClassLoaderResolver classLoaderResolver;

    @Before
    public void setUp() throws Exception {
        classLoaderResolver = new ClassLoaderResolver(log, repositorySystem, repositorySystemSession, Collections.<RemoteRepository>emptyList());
        when(repositorySystem.collectDependencies(eq(repositorySystemSession), any(CollectRequest.class)))
                .thenReturn(new CollectResult(new CollectRequest()).setRoot(root));
        when(child.getDependency()).thenReturn(new Dependency(new DefaultArtifact(FOO,
                BAR,
                QUX,
                BAZ,
                FOO + BAR,
                Collections.<String, String>emptyMap(),
                new File(FOO + "/" + BAR)), QUX + BAZ));
        when(root.accept(any(DependencyVisitor.class))).then(new Answer<Void>() {
            public Void answer(InvocationOnMock invocationOnMock) {
                DependencyVisitor dependencyVisitor = invocationOnMock.getArgument(0);
                dependencyVisitor.visitEnter(child);
                dependencyVisitor.visitLeave(child);
                return null;
            }
        });
    }

    @Test
    public void testResolution() throws Exception {
        assertThat(classLoaderResolver.resolve(new MavenCoordinate(FOO, BAR, QUX, JAR)), sameInstance(classLoaderResolver.resolve(new MavenCoordinate(FOO, BAR, QUX, JAR))));
    }

    @Test(expected = MojoExecutionException.class)
    public void testCollectionFailure() throws Exception {
        when(repositorySystem.collectDependencies(eq(repositorySystemSession), any(CollectRequest.class)))
                .thenThrow(new DependencyCollectionException(new CollectResult(new CollectRequest())));
        classLoaderResolver.resolve(new MavenCoordinate(FOO, BAR, QUX, JAR));
    }

    @Test(expected = MojoFailureException.class)
    public void testResolutionFailure() throws Exception {
        when(repositorySystem.resolveDependencies(eq(repositorySystemSession), any(DependencyRequest.class)))
                .thenThrow(new DependencyResolutionException(new DependencyResult(new DependencyRequest(root, mock(DependencyFilter.class))), new Throwable()));
        classLoaderResolver.resolve(new MavenCoordinate(FOO, BAR, QUX, JAR));
    }

    @Test
    public void testClose() throws Exception {
        classLoaderResolver.resolve(new MavenCoordinate(FOO, BAR, QUX, JAR));
        classLoaderResolver.close();
    }
}
