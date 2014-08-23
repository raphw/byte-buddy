package net.bytebuddy.dynamic.loading;

import net.bytebuddy.utility.ClassFileExtraction;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteArrayClassLoaderTest {

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    private static final String BAR = "bar";
    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        Map<String, byte[]> values = Collections.singletonMap(Foo.class.getName(), ClassFileExtraction.extract(Foo.class));
        classLoader = new ByteArrayClassLoader(BOOTSTRAP_CLASS_LOADER, values);
    }

    @Test
    public void testSuccessfulHit() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNonSuccessfulHit() throws Exception {
        // Note: Will throw a class format error instead targeting not found exception targeting loader attempts.
        classLoader.loadClass(BAR);
    }

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }
}
