package net.bytebuddy.dynamic.loading;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;

@RunWith(Parameterized.class)
public class ByteArrayClassLoaderChildFirstTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", CLASS_FILE = ".class";

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

    private final Matcher<InputStream> expectedResourceLookup;

    private ClassLoader classLoader;

    @Mock
    private PackageDefinitionStrategy packageDefinitionStrategy;

    public ByteArrayClassLoaderChildFirstTest(ByteArrayClassLoader.PersistenceHandler persistenceHandler,
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
        Map<String, byte[]> values = Collections.singletonMap(Foo.class.getName(),
                ClassFileExtraction.extract(Bar.class, new RenamingWrapper(Bar.class.getName().replace('.', '/'),
                        Foo.class.getName().replace('.', '/'))));
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                values,
                DEFAULT_PROTECTION_DOMAIN,
                persistenceHandler,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    public void testSuccessfulHit() throws Exception {
        Class<?> type = classLoader.loadClass(Foo.class.getName());
        assertThat(type.getClassLoader(), is(classLoader));
        assertEquals(classLoader.loadClass(Foo.class.getName()), type);
        assertNotEquals(Foo.class, type);
        assertThat(type.getPackage(), notNullValue(Package.class));
        assertThat(type.getPackage(), is(Foo.class.getPackage()));;
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

    public static class Foo {
        /* empty */
    }

    public static class Bar {
        /* empty */
    }

    private static class RenamingWrapper implements ClassVisitorWrapper {

        private final String oldName, newName;

        private RenamingWrapper(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public ClassVisitor wrap(ClassVisitor classVisitor) {
            return new RemappingClassAdapter(classVisitor, new SimpleRemapper(oldName, newName));
        }
    }
}
