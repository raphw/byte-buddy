package net.bytebuddy.dynamic.loading;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ByteArrayClassLoaderTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", CLASS_FILE = ".class";

    private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

    private final boolean expectedResourceLookup;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private InjectionClassLoader classLoader;

    private URL sealBase;

    @Mock
    private PackageDefinitionStrategy packageDefinitionStrategy;

    @Mock
    private ClassFileTransformer classFileTransformer;

    public ByteArrayClassLoaderTest(ByteArrayClassLoader.PersistenceHandler persistenceHandler, boolean expectedResourceLookup) {
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
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Foo.class),
                DEFAULT_PROTECTION_DOMAIN,
                persistenceHandler,
                packageDefinitionStrategy,
                classFileTransformer);
        sealBase = new URL("file://foo");
        when(packageDefinitionStrategy.define(classLoader, Foo.class.getPackage().getName(), Foo.class.getName()))
                .thenReturn(new PackageDefinitionStrategy.Definition.Simple(FOO, BAR, QUX, QUX, FOO, BAR, sealBase));
        when(packageDefinitionStrategy.define(classLoader, Bar.class.getPackage().getName(), Bar.class.getName()))
                .thenReturn(PackageDefinitionStrategy.Definition.Trivial.INSTANCE);
        when(classFileTransformer.transform(eq(classLoader),
                anyString(),
                Mockito.any(Class.class),
                Mockito.any(ProtectionDomain.class),
                Mockito.any(byte[].class))).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                return (byte[]) invocation.getArguments()[4];
            }
        });
    }

    @Test
    public void testLoading() throws Exception {
        Class<?> type = classLoader.loadClass(Foo.class.getName());
        assertThat(type.getClassLoader(), is((ClassLoader) classLoader));
        assertEquals(classLoader.loadClass(Foo.class.getName()), type);
        assertThat(type, not(CoreMatchers.<Class<?>>is(Foo.class)));
    }

    @Test
    @IntegrationRule.Enforce
    public void testPackageDefinition() throws Exception {
        Class<?> type = classLoader.loadClass(Foo.class.getName());
        assertThat(type.getPackage(), notNullValue(Package.class));
        assertThat(type.getPackage(), not(Foo.class.getPackage()));
        assertThat(type.getPackage().getName(), is(Foo.class.getPackage().getName()));
        assertThat(type.getPackage().getSpecificationTitle(), is(FOO));
        assertThat(type.getPackage().getSpecificationVersion(), is(BAR));
        assertThat(type.getPackage().getSpecificationVendor(), is(QUX));
        assertThat(type.getPackage().getImplementationTitle(), is(QUX));
        assertThat(type.getPackage().getImplementationVersion(), is(FOO));
        assertThat(type.getPackage().getImplementationVendor(), is(BAR));
        assertThat(type.getPackage().isSealed(), is(true));
        assertThat(type.getPackage().isSealed(sealBase), is(true));
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
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is((ClassLoader) classLoader));
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
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is((ClassLoader) classLoader));
        assertThat(classLoader.getResource(Foo.class.getName().replace('.', '/') + CLASS_FILE), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourcesLookupBeforeLoading() throws Exception {
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + CLASS_FILE);
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test
    public void testResourcesLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is((ClassLoader) classLoader));
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + CLASS_FILE);
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
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is((ClassLoader) classLoader));
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
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is((ClassLoader) classLoader));
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

    @Test
    public void testPackage() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getPackage().getName(), is(Foo.class.getPackage().getName()));
        assertThat(classLoader.loadClass(Foo.class.getName()).getPackage(), not(Foo.class.getPackage()));
    }

    @Test
    public void testInjection() throws Exception {
        assertThat(classLoader.defineClass(Bar.class.getName(), ClassFileLocator.ForClassLoader.read(Bar.class).resolve()).getName(), is(Bar.class.getName()));
    }

    @Test
    public void testDuplicateInjection() throws Exception {
        Class<?> type = classLoader.defineClass(Bar.class.getName(), ClassFileLocator.ForClassLoader.read(Bar.class).resolve());
        assertThat(classLoader.defineClass(Bar.class.getName(), ClassFileLocator.ForClassLoader.read(Bar.class).resolve()), is((Object) type));
    }

    @Test
    public void testPredefinedInjection() throws Exception {
        Class<?> type = classLoader.defineClass(Foo.class.getName(), ClassFileLocator.ForClassLoader.read(Foo.class).resolve());
        assertThat(type, is((Object) classLoader.loadClass(Foo.class.getName())));
    }

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }

    private static class Bar {
        /* Note: Bar is know to the system class loader but not to the bootstrap class loader */
    }
}
