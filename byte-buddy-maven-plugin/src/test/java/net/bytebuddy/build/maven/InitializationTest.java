package net.bytebuddy.build.maven;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.test.utility.MockitoRule;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InitializationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", JAR = "jar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoaderResolver classLoaderResolver;

    @Test
    public void testResolved() throws Exception {
        Initialization initalization = new Initialization();
        initalization.groupId = BAR;
        initalization.artifactId = QUX;
        initalization.version = BAZ;
        initalization.packaging = JAR;
        assertThat(initalization.getGroupId(FOO), is(BAR));
        assertThat(initalization.getArtifactId(FOO), is(QUX));
        assertThat(initalization.getVersion(FOO), is(BAZ));
        assertThat(initalization.getPackaging(JAR), is(JAR));
    }

    @Test
    public void testRebase() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = EntryPoint.Default.REBASE.name();
        assertThat(initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REBASE));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefine() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = EntryPoint.Default.REDEFINE.name();
        assertThat(initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REDEFINE));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefineLocal() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = EntryPoint.Default.REDEFINE_LOCAL.name();
        assertThat(initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), is((EntryPoint) EntryPoint.Default.REDEFINE_LOCAL));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testCustom() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = Foo.class.getName();
        when(classLoaderResolver.resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR))).thenReturn(Foo.class.getClassLoader());
        assertThat(initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR), instanceOf(Foo.class));
        verify(classLoaderResolver).resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test(expected = MojoExecutionException.class)
    public void testCustomFailed() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = FOO;
        when(classLoaderResolver.resolve(new MavenCoordinate(BAR, QUX, BAZ, JAR))).thenReturn(Foo.class.getClassLoader());
        initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test(expected = MojoExecutionException.class)
    public void testEmpty() throws Exception {
        Initialization initalization = new Initialization();
        initalization.entryPoint = "";
        initalization.getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test(expected = MojoExecutionException.class)
    public void testNull() throws Exception {
        new Initialization().getEntryPoint(classLoaderResolver, BAR, QUX, BAZ, JAR);
    }

    @Test
    public void testDefault() throws Exception {
        Initialization initialization = Initialization.makeDefault();
        assertThat(initialization.entryPoint, is(EntryPoint.Default.REBASE.name()));
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
        assertThat(initialization.asCoordinate(FOO, FOO, FOO, JAR), is(new MavenCoordinate(BAR, QUX, BAZ , JAR)));
    }

    @Test
    public void testAsCoordinateUnresolved() throws Exception {
        Initialization initialization = new Initialization();
        assertThat(initialization.asCoordinate(BAR, QUX, BAZ, JAR), is(new MavenCoordinate(BAR, QUX, BAZ , JAR)));
    }

    public static class Foo implements EntryPoint {

        @Override
        public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
            throw new AssertionError();
        }

        @Override
        public DynamicType.Builder<?> transform(TypeDescription typeDescription, ByteBuddy byteBuddy,
                                                ClassFileLocator classFileLocator,
                                                MethodNameTransformer methodNameTransformer) {
            throw new AssertionError();
        }
    }
}
