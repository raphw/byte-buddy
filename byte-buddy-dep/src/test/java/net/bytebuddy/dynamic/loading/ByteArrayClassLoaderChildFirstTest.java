package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ByteArrayClassLoaderChildFirstTest {

    private static final String BAR = "bar";

    private final ByteArrayClassLoader.PersistenceHandler persistenceHandler;

    private final boolean expectedResourceLookup;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                Collections.singletonMap(Foo.class.getName(), new ByteBuddy()
                        .redefine(Bar.class)
                        .visit(new RenamingWrapper(Bar.class.getName().replace('.', '/'), Foo.class.getName().replace('.', '/')))
                        .make()
                        .getBytes()),
                ClassLoadingStrategy.NO_PROTECTION_DOMAIN,
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
        // Due to change in API in Java 9 where package identity is no longer bound by hierarchy.
        assertThat(type.getPackage(), ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
                ? not(is(Foo.class.getPackage()))
                : is(Foo.class.getPackage()));
    }

    @Test
    public void testResourceStreamLookupBeforeLoading() throws Exception {
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
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
        InputStream inputStream = classLoader.getResourceAsStream(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
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
        assertThat(classLoader.getResource(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourceLookupAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        assertThat(classLoader.getResource(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourcesLookupBeforeLoading() throws Exception {
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
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
        Enumeration<URL> enumeration = classLoader.getResources(Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
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
        assertThat(classLoader.getResource("/" + Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourceLookupWithPrefixAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        assertThat(classLoader.getResource("/" + Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION), expectedResourceLookup
                ? notNullValue(URL.class)
                : nullValue(URL.class));
    }

    @Test
    public void testResourcesLookupWithPrefixBeforeLoading() throws Exception {
        Enumeration<URL> enumeration = classLoader.getResources("/" + Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
        assertThat(enumeration.hasMoreElements(), is(expectedResourceLookup));
        if (expectedResourceLookup) {
            assertThat(enumeration.nextElement(), notNullValue(URL.class));
            assertThat(enumeration.hasMoreElements(), is(false));
        }
    }

    @Test
    public void testResourcesLookupWithPrefixAfterLoading() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        Enumeration<URL> enumeration = classLoader.getResources("/" + Foo.class.getName().replace('.', '/') + ClassFileLocator.CLASS_FILE_EXTENSION);
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

        public int mergeWriter(int flags) {
            return flags;
        }

        public int mergeReader(int flags) {
            return flags;
        }

        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            return new ClassRemapper(OpenedClassReader.ASM_API, classVisitor, new SimpleRemapper(OpenedClassReader.ASM_API, oldName, newName)) {
                /* only anonymous to define usage of Byte Buddy specific API version */
            };
        }
    }
}
