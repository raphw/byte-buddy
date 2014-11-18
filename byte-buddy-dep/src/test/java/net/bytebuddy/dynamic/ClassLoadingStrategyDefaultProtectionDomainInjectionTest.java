package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ClassFileExtraction;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassLoadingStrategyDefaultProtectionDomainInjectionTest {

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
        protectionDomain = Foo.class.getProtectionDomain();
    }

    @Test
    public void testProtectionDomainInjection() throws Exception {
        Map<TypeDescription, Class<?>> loaded = new ClassLoadingStrategy.Default.ProtectionDomainInjection(protectionDomain)
                .load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassLoadingStrategy.Default.ProtectionDomainInjection.class).apply();
    }

    private static class Foo {
        /* empty */
    }
}
