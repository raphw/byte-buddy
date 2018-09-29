package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.*;
import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.Convention;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransformationActionTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", TEMP = ".tmp";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Project project;

    @Mock
    private Convention convention;

    @Mock
    private Logger logger;

    @Mock
    private ByteBuddyExtension byteBuddyExtension;

    @Mock
    private AbstractCompile parent;

    @Mock
    private Task task;

    @Mock
    private Transformation transformation;

    @Mock
    private Initialization initialization;

    @Mock
    private FileCollection fileCollection;

    private File target;

    private TransformationAction transformationAction;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        target = File.createTempFile(FOO, TEMP);
        assertThat(target.delete(), is(true));
        assertThat(target.mkdir(), is(true));
        when(project.getConvention()).thenReturn(convention);
        when(convention.getPlugins()).thenReturn(Collections.<String, Object>emptyMap());
        when(project.getLogger()).thenReturn(logger);
        when(byteBuddyExtension.getTransformations()).thenReturn(Collections.singletonList(transformation));
        when(byteBuddyExtension.getInitialization()).thenReturn(initialization);
        when(parent.getDestinationDir()).thenReturn(target);
        when(transformation.getClassPath(any(File.class), any(Iterable.class))).thenReturn((Iterable) Collections.emptySet());
        when(parent.getClasspath()).thenReturn(fileCollection);
        when(fileCollection.iterator()).then(new Answer<Iterator<File>>() {
            public Iterator<File> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.singleton(target).iterator();
            }
        });
        when(byteBuddyExtension.getMethodNameTransformer()).thenReturn(MethodNameTransformer.Suffixing.withRandomSuffix());
        when(transformation.makeArgumentResolvers()).thenReturn(Collections.<Plugin.Factory.UsingReflection.ArgumentResolver>emptyList());
        transformationAction = new TransformationAction(project, byteBuddyExtension, parent);
    }

    @After
    public void tearDown() throws Exception {
        assertThat(target.delete(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleTransformation() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            when(transformation.getPlugin()).thenReturn(SimplePlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleTransformationWithArgument() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            when(transformation.getPlugin()).thenReturn(ArgumentPlugin.class.getName());
            when(transformation.makeArgumentResolvers()).thenReturn(Collections.singletonList(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(int.class, 42)));
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, "42");
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLiveInitializer() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        try {
            when(transformation.getPlugin()).thenReturn(LiveInitializerPlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
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
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLiveInitializerAllowed() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        try {
            when(transformation.getPlugin()).thenReturn(LiveInitializerPlugin.class.getName());
            when(byteBuddyExtension.isFailOnLiveInitializer()).thenReturn(false);
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
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
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test(expected = GradleException.class)
    @SuppressWarnings("unchecked")
    public void testIllegalTransformer() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        try {
            when(transformation.getPlugin()).thenReturn(IllegalTransformPlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test(expected = GradleException.class)
    @SuppressWarnings("unchecked")
    public void testIllegalTransformation() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        try {
            when(transformation.getPlugin()).thenReturn(IllegalPlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(EntryPoint.Default.REBASE);
            transformationAction.execute(task);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleEntry() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            when(transformation.getPlugin()).thenReturn(SimplePlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(new SimpleEntryPoint());
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test(expected = GradleException.class)
    @SuppressWarnings("unchecked")
    public void testIllegalByteBuddy() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            when(transformation.getPlugin()).thenReturn(SimplePlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(new IllegalEntryPoint());
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test(expected = GradleException.class)
    @SuppressWarnings("unchecked")
    public void testIllegalTransform() throws Exception {
        Set<File> files = new HashSet<File>();
        files.addAll(addClass("foo.Bar"));
        files.addAll(addClass("foo.Qux"));
        try {
            when(transformation.getPlugin()).thenReturn(SimplePlugin.class.getName());
            when(initialization.getEntryPoint(any(ClassLoaderResolver.class), any(File.class), any(Iterable.class))).thenReturn(new IllegalTransformEntryPoint());
            transformationAction.execute(task);
            ClassLoader classLoader = new URLClassLoader(new URL[]{target.toURI().toURL()});
            assertMethod(classLoader.loadClass("foo.Bar"), FOO, QUX);
            assertMethod(classLoader.loadClass("foo.Bar"), BAR, BAR);
            assertMethod(classLoader.loadClass("foo.Qux"), FOO, FOO);
            assertMethod(classLoader.loadClass("foo.Qux"), BAR, BAR);
        } finally {
            for (File file : files) {
                assertThat(file.delete(), is(true));
            }
            assertThat(new File(target, FOO).delete(), is(true));
        }
    }

    @Test(expected = GradleException.class)
    public void testNoDirectory() throws Exception {
        when(parent.getDestinationDir()).thenReturn(mock(File.class));
        transformationAction.execute(task);
    }

    private void assertMethod(Class<?> type, String name, Object expected) throws Exception {
        assertThat(type.getDeclaredMethod(name).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }

    private Collection<File> addClass(String name) throws IOException {
        return new ByteBuddy()
                .subclass(Object.class)
                .name(name)
                .defineMethod(FOO, String.class, Visibility.PUBLIC).intercept(FixedValue.value(FOO))
                .defineMethod(BAR, String.class, Visibility.PUBLIC).intercept(FixedValue.value(BAR))
                .make()
                .saveIn(target)
                .values();
    }
}
