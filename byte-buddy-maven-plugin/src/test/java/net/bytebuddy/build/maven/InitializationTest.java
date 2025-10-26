package net.bytebuddy.build.maven;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class InitializationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", JAR = "jar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClassLoaderResolver classLoaderResolver;

    @Test
    public void testResolved() throws Exception {
        Initialization initialization = new Initialization();
        initialization.groupId = BAR;
        initialization.artifactId = QUX;
        initialization.version = BAZ;
        initialization.packaging = JAR;
        assertThat(initialization.getGroupId(FOO), is(BAR));
        assertThat(initialization.getArtifactId(FOO), is(QUX));
        assertThat(initialization.getVersion(FOO), is(BAZ));
        assertThat(initialization.getPackaging(JAR), is(JAR));
    }

    @Test
    public void testRebase() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = EntryPoint.Default.REBASE.name();
        assertThat(initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REBASE));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefine() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = EntryPoint.Default.REDEFINE.name();
        assertThat(initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REDEFINE));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefineLocal() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = EntryPoint.Default.REDEFINE_LOCAL.name();
        assertThat(initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REDEFINE_LOCAL));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test
    public void testDecorate() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = EntryPoint.Default.DECORATE.name();
        assertThat(initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.DECORATE));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test
    public void testCustom() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = Foo.class.getName();
        when(classLoaderResolver.resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR))).thenReturn(Foo.class.getClassLoader());
        assertThat(initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), instanceOf(Foo.class));
        verify(classLoaderResolver).resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test(expected = MojoExecutionException.class)
    public void testCustomFailed() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = FOO;
        when(classLoaderResolver.resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR))).thenReturn(Foo.class.getClassLoader());
        initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test(expected = MojoExecutionException.class)
    public void testNull() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = null;
        initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test(expected = MojoExecutionException.class)
    public void testEmpty() throws Exception {
        Initialization initialization = new Initialization();
        initialization.entryPoint = "";
        initialization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test
    public void testDefault() throws Exception {
        Initialization initialization = new Initialization();
        assertThat(initialization.entryPoint, is(EntryPoint.Default.REBASE.name()));
        assertThat(initialization.validated, is(true));
        assertThat(initialization.groupId, nullValue(String.class));
        assertThat(initialization.artifactId, nullValue(String.class));
        assertThat(initialization.version, nullValue(String.class));
    }

    @Test
    public void testAsCoordinateResolved() throws Exception {
        Initialization initialization = new Initialization();
        initialization.groupId = BAR;
        initialization.artifactId = QUX;
        initialization.version = BAZ;
        assertThat(initialization.asCoordinate(FOO, FOO, FOO, JAR), is(new MavenCoordinate(BAR, QUX, BAZ, JAR)));
    }

    @Test
    public void testAsCoordinateUnresolved() throws Exception {
        Initialization initialization = new Initialization();
        assertThat(initialization.asCoordinate(BAR, QUX, BAZ, JAR), is(new MavenCoordinate(BAR, QUX, BAZ, JAR)));
    }

    public static class Foo implements EntryPoint {

        private static final long serialVersionUID = 1L;

        public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> transform(TypeDescription typeDescription, ByteBuddy byteBuddy,
                                                ClassFileLocator classFileLocator,
                                                MethodNameTransformer methodNameTransformer) {
            throw new AssertionError();
        }
    }
}
