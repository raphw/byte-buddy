package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ClassFileExtraction;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassLoadingStrategyDefaultTest {

    private static class Foo {
        /* empty */
    }

    private ClassLoader classLoader;
    private TypeDescription typeDescription;
    private Map<TypeDescription, byte[]> binaryRepresentations;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* bootstrap class loader */);
        binaryRepresentations = new LinkedHashMap<TypeDescription, byte[]>();
        typeDescription = new TypeDescription.ForLoadedType(Foo.class);
        binaryRepresentations.put(typeDescription, ClassFileExtraction.extract(Foo.class));
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
    public void testInjection() throws Exception {
        Map<TypeDescription, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.load(classLoader, binaryRepresentations);
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }
}
