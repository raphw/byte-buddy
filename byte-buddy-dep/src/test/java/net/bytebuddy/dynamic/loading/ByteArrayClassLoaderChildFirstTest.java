package net.bytebuddy.dynamic.loading;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ByteArrayClassLoaderChildFirstTest {

    private static final String BAR = "bar", CLASS_FILE = ".class";

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

    private final boolean expectedResourceLookup;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private ClassLoader classLoader;

    @Mock
    private PackageDefinitionStrategy packageDefinitionStrategy;

    public ByteArrayClassLoaderChildFirstTest(ByteArrayClassLoader.PersistenceHandler persistenceHandler, boolean expectedResourceLookup) {
        this.persistenceHandler = persistenceHandler;
        this.expectedResourceLookup = expectedResourceLookup;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ByteArrayClassLoader.PersistenceHandler.LATENT, false},
                {ByteArrayClassLoader.PersistenceHandler.MANIFEST, true}
        });
    }

    @Before
    public void setUp() throws Exception {
        Map<String, byte[]> values = Collections.singletonMap(Foo.class.getName(),
                ClassFileExtraction.extract(Bar.class, new RenamingWrapper(Bar.class.getName().replace('.', '/'),
                        Foo.class.getName().replace('.', '/'))));
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                values,
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                persistenceHandler,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    public void testLoading() throws Exception {
        Class<?> type = classLoader.loadClass(Foo.class.getName());
        assertThat(type.getClassLoader(), is(classLoader));
        assertEquals(classLoader.loadClass(Foo.class.getName()), type);
        assertThat(type, not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(type.getPackage(), notNullValue(Package.class));
        assertThat(type.getPackage(), is(Foo.class.getPackage()));
    }

    @Test
    public void testResourceStreamLookupBeforeLoading() throws Exception {
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        try {
            assertThat(inputStream, expectedResourceLookup ? notNullValue(InputStream.class) : nullValue(InputStream.class));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Test
    public void testResourceStreamLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        try {
            assertThat(inputStream, expectedResourceLookup ? notNullValue(InputStream.class) : nullValue(InputStream.class));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Test
    public void testResourceLookupBeforeLoading() throws Exception {
        assertThat(classLoader.getResource(Foo.class.getName().replace('.', '/') + CLASS_FILE), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourceLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        assertThat(classLoader.getResource(Foo.class.getName().replace('.', '/') + CLASS_FILE), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourcesLookupBeforeLoading() throws Exception {
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), notNullValue(URL.class));
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test
    public void testResourcesLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        assertThat(enumeration.hasMoreElements(), is(true));
        assertThat(enumeration.nextElement(), notNullValue(URL.class));
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test
    public void testResourceLookupWithPrefixBeforeLoading() throws Exception {
        assertThat(classLoader.getResource("/" + Foo.class.getName().replace('.', '/') + CLASS_FILE), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourceLookupWithPrefixAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        assertThat(classLoader.getResource("/" + Foo.class.getName().replace('.', '/') + CLASS_FILE), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourcesLookupWithPrefixBeforeLoading() throws Exception {
        Enumeration<URL> enumeration = classLoader.getResources("/" + Foo.class.getName().replace('.', '/') + CLASS_FILE);
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test
    public void testResourcesLookupWithPrefixAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        Enumeration<URL> enumeration = classLoader.getResources("/" + Foo.class.getName().replace('.', '/') + CLASS_FILE);
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNotFoundException() throws Exception {
        // Note: Will throw a class format error instead targeting not found exception targeting loader attempts.
        classLoader.loadClass(BAR);
    }

    public static class Foo {
        /* empty */
    }

    public static class Bar {
        /* empty */
    }

    private static class RenamingWrapper implements AsmVisitorWrapper {

        private final String oldName, newName;

        private RenamingWrapper(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public int mergeWriter(int flags) {
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags;
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
            return new ClassRemapper(classVisitor, new SimpleRemapper(oldName, newName));
        }
    }
}
