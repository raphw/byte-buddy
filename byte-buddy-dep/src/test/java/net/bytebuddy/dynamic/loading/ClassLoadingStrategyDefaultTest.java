package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClassLoadingStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private ClassLoader classLoader;

    private TypeDescription typeDescription;

    private Map<TypeDescription, byte[]> binaryRepresentations;

    private ProtectionDomain protectionDomain;

    @Mock
    private PackageDefinitionStrategy packageDefinitionStrategy;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null);
        binaryRepresentations = new LinkedHashMap<TypeDescription, byte[]>();
        typeDescription = new TypeDescription.ForLoadedType(Foo.class);
        binaryRepresentations.put(typeDescription, ClassFileExtraction.extract(Foo.class));
        protectionDomain = getClass().getProtectionDomain();
        when(packageDefinitionStrategy.define(any(ClassLoader.class), any(String.class), any(String.class)))
                .thenReturn(PackageDefinitionStrategy.Definition.Undefined.INSTANCE);
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
    public void testWrapperWithPackageDefiner() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.withPackageDefinitionStrategy(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testWrapperPersistentWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.withPackageDefinitionStrategy(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testChildFirstWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST.withPackageDefinitionStrategy(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testChildFirstPersistentWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT.withPackageDefinitionStrategy(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testInjectionWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.withPackageDefinitionStrategy(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassLoadingStrategy.Default.class).apply();
        ObjectPropertyAssertion.of(ClassLoadingStrategy.Default.WrappingDispatcher.class).apply();
        ObjectPropertyAssertion.of(ClassLoadingStrategy.Default.InjectionDispatcher.class).apply();
    }

    private static class Foo {
        /* empty */
    }
}
