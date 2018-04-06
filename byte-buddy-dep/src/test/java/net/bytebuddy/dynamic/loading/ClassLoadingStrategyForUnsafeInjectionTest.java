package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ClassUnsafeInjectionAvailableRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassLoadingStrategyForUnsafeInjectionTest {

    @Rule
    public MethodRule classUnsafeInjectionAvailableRule = new ClassUnsafeInjectionAvailableRule();

    private ClassLoader classLoader;

    private TypeDescription typeDescription;

    private Map<TypeDescription, byte[]> binaryRepresentations;

    private ProtectionDomain protectionDomain;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null);
        binaryRepresentations = new LinkedHashMap<TypeDescription, byte[]>();
        typeDescription = new TypeDescription.ForLoadedType(Foo.class);
        binaryRepresentations.put(typeDescription, ClassFileExtraction.extract(Foo.class));
        protectionDomain = getClass().getProtectionDomain();
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testInjection() throws Exception {
        Map<TypeDescription, Class<?>> loaded = new ClassLoadingStrategy.ForUnsafeInjection().load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testInjectionWithProtectionDomain() throws Exception {
        Map<TypeDescription, Class<?>> loaded = new ClassLoadingStrategy.ForUnsafeInjection(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testInjectionDoesNotThrowExceptionOnExistingClass() throws Exception {
        Map<TypeDescription, Class<?>> types = new ClassLoadingStrategy.ForUnsafeInjection(protectionDomain)
                .load(ClassLoader.getSystemClassLoader(), Collections.singletonMap(TypeDescription.STRING, new byte[0]));
        assertThat(types.size(), is(1));
        assertEquals(String.class, types.get(TypeDescription.STRING));
    }

    private static class Foo {
        /* empty */
    }
}
