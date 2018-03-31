package net.bytebuddy.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ClassFileLocatorForClassLoaderTest {

    private static final String FOOBAR = "foo/bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClosableClassLoader classLoader;

    @Test
    public void testCreation() throws Exception {
        assertThat(ClassFileLocator.ForClassLoader.of(classLoader), hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader(classLoader)));
        assertThat(ClassFileLocator.ForClassLoader.of(null), hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader(ClassLoader.getSystemClassLoader())));
    }

    @Test
    public void testLocatable() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(classLoader.getResourceAsStream(FOOBAR + ".class")).thenReturn(inputStream);
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForClassLoader(classLoader)
                .locate(FOOBAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{1, 2, 3}));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonLocatable() throws Exception {
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForClassLoader(classLoader)
                .locate(FOOBAR);
        assertThat(resolution.isResolved(), is(false));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
        resolution.resolve();
        fail();
    }

    @Test
    public void testReadTypeBootstrapClassLoader() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.ForClassLoader.read(Object.class);
        assertThat(resolution.isResolved(), is(true));
        JavaModule module = JavaModule.ofType(Object.class);
        InputStream inputStream = module == null
                ? Object.class.getResourceAsStream(Object.class.getSimpleName() + ".class")
                : module.getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(resolution.resolve(), is(StreamDrainer.DEFAULT.drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testReadTypeNonBootstrapClassLoader() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.ForClassLoader.read(Foo.class);
        assertThat(resolution.isResolved(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(resolution.resolve(), is(StreamDrainer.DEFAULT.drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testReadTypeIllegal() throws Exception {
        Class<?> nonClassFileType = new ByteBuddy().subclass(Object.class).make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        ClassFileLocator.Resolution resolution = ClassFileLocator.ForClassLoader.read(nonClassFileType);
        assertThat(resolution.isResolved(), is(false));
        resolution.resolve();
    }

    @Test
    public void testClose() throws Exception {
        ClassFileLocator.ForClassLoader.of(classLoader).close();
        verifyZeroInteractions(classLoader);
    }

    @Test
    public void testSystemClassLoader() throws Exception {
        assertThat(ClassFileLocator.ForClassLoader.ofClassPath(),
                hasPrototype(ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader())));
    }

    @Test
    public void testPlatformLoader() throws Exception {
        assertThat(ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader().getParent()),
                hasPrototype(ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader().getParent())));
    }

    @Test
    public void testBootLoader() throws Exception {
        assertThat(ClassFileLocator.ForClassLoader.of(null),
                hasPrototype(ClassFileLocator.ForClassLoader.of(ClassLoader.getSystemClassLoader())));
    }

    private static class Foo {
        /* empty */
    }

    private abstract static class ClosableClassLoader extends ClassLoader implements Closeable {
        /* empty */
    }
}
