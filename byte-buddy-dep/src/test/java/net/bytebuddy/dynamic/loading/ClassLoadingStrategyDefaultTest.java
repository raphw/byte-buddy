package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ClassInjectionAvailableRule;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassLoadingStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Rule
    public MethodRule classInjectionAvailableRule = new ClassInjectionAvailableRule();

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
    @ClassInjectionAvailableRule.Enforce
    public void testInjection() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.with(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperPersistentWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirstWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST.with(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testChildFirstPersistentWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT.with(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    @ClassInjectionAvailableRule.Enforce
    public void testInjectionWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.with(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testWrapperWithPackageDefiner() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.with(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testWrapperPersistentWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testChildFirstWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST.with(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    public void testChildFirstPersistentWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT.with(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test
    @ClassInjectionAvailableRule.Enforce
    public void testInjectionWithPackageDefinitionStrategy() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.with(packageDefinitionStrategy)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
        verify(packageDefinitionStrategy).define(any(ClassLoader.class), eq(Foo.class.getPackage().getName()), eq(Foo.class.getName()));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrapperThrowsExceptionOnExistingClass() throws Exception {
        ClassLoadingStrategy.Default.WRAPPER.load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrapperPersistentThrowsExceptionOnExistingClass() throws Exception {
        ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
    }

    @Test(expected = IllegalStateException.class)
    public void testInjectionThrowsExceptionOnExistingClass() throws Exception {
        ClassLoadingStrategy.Default.INJECTION.load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
    }

    @Test
    public void testWrapperDoesNotThrowExceptionOnExistingClassWhenSupressed() throws Exception {
        Map<TypeDescription, Class<?>> types = ClassLoadingStrategy.Default.WRAPPER
                .allowExistingTypes()
                .load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
        assertThat(types.size(), is(1));
        assertEquals(String.class, types.get(TypeDescription.STRING));
    }

    @Test
    public void testWrapperPersistentDoesNotThrowExceptionOnExistingClassWhenSupressed() throws Exception {
        Map<TypeDescription, Class<?>> types = ClassLoadingStrategy.Default.WRAPPER_PERSISTENT
                .allowExistingTypes()
                .load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
        assertThat(types.size(), is(1));
        assertEquals(String.class, types.get(TypeDescription.STRING));
    }

    @Test
    public void testInjectionDoesNotThrowExceptionOnExistingClassWhenSupressed() throws Exception {
        Map<TypeDescription, Class<?>> types = ClassLoadingStrategy.Default.INJECTION
                .allowExistingTypes()
                .load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
        assertThat(types.size(), is(1));
        assertEquals(String.class, types.get(TypeDescription.STRING));
    }

    private static class Foo {
        /* empty */
    }
}
