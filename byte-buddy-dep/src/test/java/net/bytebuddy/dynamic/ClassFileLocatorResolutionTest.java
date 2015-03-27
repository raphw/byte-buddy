package net.bytebuddy.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.StreamDrainer;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorResolutionTest {

    private static final byte[] DATA = new byte[]{1, 2, 3};

    @Test
    public void testIllegal() throws Exception {
        MatcherAssert.assertThat(ClassFileLocator.Resolution.Illegal.INSTANCE.isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowsException() throws Exception {
        ClassFileLocator.Resolution.Illegal.INSTANCE.resolve();
    }

    @Test
    public void testExplicit() throws Exception {
        assertThat(new ClassFileLocator.Resolution.Explicit(DATA).isResolved(), is(true));
    }

    @Test
    public void testExplicitGetData() throws Exception {
        assertThat(new ClassFileLocator.Resolution.Explicit(DATA).resolve(), is(DATA));
    }

    @Test
    public void testReadTypeBootstrapClassLoader() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Resolution.Explicit.of(Object.class);
        assertThat(resolution.isResolved(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(resolution.resolve(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testReadTypeNonBootstrapClassLoader() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Resolution.Explicit.of(Foo.class);
        assertThat(resolution.isResolved(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(resolution.resolve(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testReadTypeIllegal() throws Exception {
        Class<?> nonClassFileType = new ByteBuddy().subclass(Object.class).make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        ClassFileLocator.Resolution resolution = ClassFileLocator.Resolution.Explicit.of(nonClassFileType);
        assertThat(resolution.isResolved(), is(false));
        resolution.resolve();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.Resolution.Explicit.class).apply();
        ObjectPropertyAssertion.of(ClassFileLocator.Resolution.Illegal.class).apply();
    }

    private static class Foo {

    }
}
