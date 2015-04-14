package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassLoadingStrategyDefaultTest {

    private ClassLoader classLoader;

    private TypeDescription typeDescription;

    private Map<TypeDescription, byte[]> binaryRepresentations;

    private ProtectionDomain protectionDomain;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* bootstrap class loader */);
        binaryRepresentations = new LinkedHashMap<TypeDescription, byte[]>();
        typeDescription = new TypeDescription.ForLoadedType(Foo.class);
        binaryRepresentations.put(typeDescription, ClassFileExtraction.extract(Foo.class));
        protectionDomain = getClass().getProtectionDomain();
    }

    @Test
    public void testWrapper() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperPersistent() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirst() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirstPersistent() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testInjection() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.withProtectionDomain(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperPersistentWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.withProtectionDomain(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirstWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST.withProtectionDomain(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirstPersistentWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT.withProtectionDomain(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testInjectionWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.withProtectionDomain(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassLoadingStrategy.Default.class);
    }

    private static class Foo {
        /* empty */
    }
}
