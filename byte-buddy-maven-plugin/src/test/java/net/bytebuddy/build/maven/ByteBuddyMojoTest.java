package net.bytebuddy.build.maven;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.MockitoRule;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.SilentLog;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ByteBuddyMojoTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", TEMP = "tmp", JAR = "jar";

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private DependencyNode root;

    private File project;

    @Before
    public void setUp() throws Exception {
        when(repositorySystem.collectDependencies(Mockito.<RepositorySystemSession>any(), Mockito.<CollectRequest>any())).thenReturn(new CollectResult(new CollectRequest()).setRoot(root));
        project = File.createTempFile(FOO, TEMP);
        assertThat(project.delete(), is(true));
        assertThat(project.mkdir(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        assertThat(project.delete(), is(true));
    }

    @Test
    public void testEmptyTransformation() throws Exception {
        execute("transform", "empty");
    }

    @Test
    public void testSimpleTransformation() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform", "simple");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test
    public void testSimpleTransformationWithSuffix() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform", "suffix");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertThat(classLoader.loadClass("foo.Bar").getDeclaredMethod(FOO + "$" + QUX), notNullValue(Method.class));
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testLiveInitializer() throws Exception {
        Set<File> files = new HashSet<File>(addClass("foo.Bar"));
        try {
            execute("transform", "live");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test
    public void testLiveInitializerAllowed() throws Exception {
        Set<File> files = new HashSet<File>(addClass("foo.Bar"));
        try {
            execute("transform", "live.allowed");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            try {
                assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
                fail();
            } catch (InvocationTargetException exception) {
                assertThat(exception.getCause(), instanceOf(NullPointerException.class));
            }
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testIllegalTransformer() throws Exception {
        Set<File> files = new HashSet<File>(addClass("foo.Bar"));
        try {
            execute("transform", "illegal");
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testIllegalTransformation() throws Exception {
        Set<File> files = new HashSet<File>(addClass("foo.Bar"));
        try {
            execute("transform", "illegal.apply");
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test
    public void testTestTransformation() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform-test", "simple");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test
    public void testSimpleEntry() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform", "entry");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testIllegalByteBuddy() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform", "entry.illegal");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testIllegalTransform() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            execute("transform", "entry.illegal.transform");
            ClassLoader classLoader = new URLClassLoader(new URL[]{project.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(project, FOO).delete(), is(true));
        }
    }

    private void execute(String goal, String target) throws Exception {
        InputStream in = ByteBuddyMojoTest.class.getResourceAsStream("/net/bytebuddy/test/" + target + ".pom.xml");
        if (in == null) {
            throw new AssertionError("Cannot find resource for: " + target);
        }
        try {
            File pom = File.createTempFile("maven", ".pom");
            OutputStream out = new FileOutputStream(pom);
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            } finally {
                out.close();
            }
            Mojo mojo = mojoRule.lookupMojo(goal, pom);
            if (goal.equals("transform")) {
                mojoRule.setVariableValueToObject(mojo, "outputDirectory", project.getAbsolutePath());
                mojoRule.setVariableValueToObject(mojo, "compileClasspathElements", Collections.emptyList());
            } else if (goal.equals("transform-test")) {
                mojoRule.setVariableValueToObject(mojo, "testOutputDirectory", project.getAbsolutePath());
                mojoRule.setVariableValueToObject(mojo, "testClasspathElements", Collections.emptyList());
            } else {
                throw new AssertionError("Unknown goal: " + goal);
            }
            mojoRule.setVariableValueToObject(mojo, "repositorySystem", repositorySystem);
            mojoRule.setVariableValueToObject(mojo, "groupId", FOO);
            mojoRule.setVariableValueToObject(mojo, "artifactId", BAR);
            mojoRule.setVariableValueToObject(mojo, "version", QUX);
            mojoRule.setVariableValueToObject(mojo, "packaging", JAR);
            mojo.setLog(new SilentLog());
            mojo.execute();
        } finally {
            in.close();
        }
    }

    private Collection<File> addClass(String name) throws IOException {
        return new ByteBuddy()
                .subclass(Object.class)
                .name(name)
                .defineMethod(FOO, String.class, Visibility.PUBLIC).intercept(FixedValue.value(FOO))
                .defineMethod(BAR, String.class, Visibility.PUBLIC).intercept(FixedValue.value(BAR))
                .make()
                .saveIn(project)
                .values();
    }

    private static void assertMethod(Class<?> type, String name, Object expected) throws Exception {
        assertThat(type.getDeclaredMethod(name).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }
}
