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
import java.util.Map;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
        assertThat(ClassFileLocator.ForClassLoader.of(null), not(ClassFileLocator.ForClassLoader.ofSystemLoader()));
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
        byte[] binaryRepresentation = ClassFileLocator.ForClassLoader.read(Object.class);
        JavaModule module = JavaModule.ofType(Object.class);
        InputStream inputStream = module == null
                ? Object.class.getResourceAsStream(Object.class.getSimpleName() + ".class")
                : module.getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation, is(StreamDrainer.DEFAULT.drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testReadTypeNonBootstrapClassLoader() throws Exception {
        byte[] binaryRepresentation = ClassFileLocator.ForClassLoader.read(Foo.class);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation, is(StreamDrainer.DEFAULT.drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testReadTypeIllegal() throws Exception {
        Class<?> nonClassFileType = new ByteBuddy().subclass(Object.class).make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        ClassFileLocator.ForClassLoader.read(nonClassFileType);
    }

    @Test
    public void testReadTypesMultiple() throws Exception {
        Map<Class<?>, byte[]> binaryRepresentations = ClassFileLocator.ForClassLoader.read(Object.class, Foo.class);
        assertThat(binaryRepresentations.size(), is(2));
        JavaModule module = JavaModule.ofType(Object.class);
        InputStream objectStream = module == null
                ? Object.class.getResourceAsStream(Object.class.getSimpleName() + ".class")
                : module.getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentations.get(Object.class), is(StreamDrainer.DEFAULT.drain(objectStream)));
        } finally {
            objectStream.close();
        }
        InputStream fooStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentations.get(Foo.class), is(StreamDrainer.DEFAULT.drain(fooStream)));
        } finally {
            fooStream.close();
        }
    }

    @Test
    public void testReadTypesToNames() throws Exception {
        Map<String, byte[]> binaryRepresentations = ClassFileLocator.ForClassLoader.readToNames(Object.class, Foo.class);
        assertThat(binaryRepresentations.size(), is(2));
        JavaModule module = JavaModule.ofType(Object.class);
        InputStream objectStream = module == null
                ? Object.class.getResourceAsStream(Object.class.getSimpleName() + ".class")
                : module.getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentations.get(Object.class.getName()), is(StreamDrainer.DEFAULT.drain(objectStream)));
        } finally {
            objectStream.close();
        }
        InputStream fooStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentations.get(Foo.class.getName()), is(StreamDrainer.DEFAULT.drain(fooStream)));
        } finally {
            fooStream.close();
        }
    }

    @Test
    public void testClose() throws Exception {
        ClassFileLocator.ForClassLoader.of(classLoader).close();
        verifyZeroInteractions(classLoader);
    }

    @Test
    public void testSystemClassLoader() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.ofSystemLoader();
        assertThat(classFileLocator.locate(getClass().getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate("foo.Bar").isResolved(), is(false));
    }

    @Test
    public void testPlatformLoader() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.ofPlatformLoader();
        assertThat(classFileLocator.locate(getClass().getName()).isResolved(), is(false));
        assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate("foo.Bar").isResolved(), is(false));
    }

    @Test
    public void testBootLoader() throws Exception {
        ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.ofBootLoader();
        assertThat(classFileLocator.locate(getClass().getName()).isResolved(), is(false));
        assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        assertThat(classFileLocator.locate("foo.Bar").isResolved(), is(false));
    }

    private static class Foo {
        /* empty */
    }

    private abstract static class ClosableClassLoader extends ClassLoader implements Closeable {
        /* empty */
    }
}
