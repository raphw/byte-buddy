package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ClassFileExtraction;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;

@RunWith(Parameterized.class)
public class ByteArrayClassLoaderTest {

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String BAR = "bar", CLASS_FILE = ".class";

    private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

    private final Matcher<InputStream> expectedResourceLookup;

    private ClassLoader classLoader;

    public ByteArrayClassLoaderTest(ByteArrayClassLoader.PersistenceHandler persistenceHandler,
                                    Matcher<InputStream> expectedResourceLookup) {
        this.persistenceHandler = persistenceHandler;
        this.expectedResourceLookup = expectedResourceLookup;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ByteArrayClassLoader.PersistenceHandler.LATENT, nullValue(InputStream.class)},
                {ByteArrayClassLoader.PersistenceHandler.MANIFEST, notNullValue(InputStream.class)}
        });
    }

    @Before
    public void setUp() throws Exception {
        Map<String, byte[]> values = Collections.singletonMap(Foo.class.getName(), ClassFileExtraction.extract(Foo.class));
        classLoader = new ByteArrayClassLoader(BOOTSTRAP_CLASS_LOADER, values, DEFAULT_PROTECTION_DOMAIN, persistenceHandler);
    }

    @Test
    public void testSuccessfulHit() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        assertNotEquals(Foo.class, classLoader.loadClass(Foo.class.getName()));
    }

    @Test
    public void testResourceLookupBeforeLoading() throws Exception {
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        try {
            assertThat(inputStream, expectedResourceLookup);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Test
    public void testResourceLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        try {
            assertThat(inputStream, expectedResourceLookup);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
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
