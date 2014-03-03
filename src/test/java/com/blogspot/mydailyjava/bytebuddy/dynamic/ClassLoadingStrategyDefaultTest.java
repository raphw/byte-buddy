package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.utility.ClassFileExtraction;
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
    private LinkedHashMap<String, byte[]> binaryRepresentations;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* bootstrap class loader */);
        binaryRepresentations = new LinkedHashMap<String, byte[]>();
        binaryRepresentations.put(Foo.class.getName(), ClassFileExtraction.extract(Foo.class));
    }

    @Test
    public void testWrapper() throws Exception {
        Map<String, Class<?>> loaded = ClassLoadingStrategy.Default.WRAPPER.load(classLoader, binaryRepresentations);
        Class<?> type = loaded.get(Foo.class.getName());
        assertThat(loaded.size(), is(1));
        assertThat(type.getClassLoader().getParent(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testInjection() throws Exception {
        Map<String, Class<?>> loaded = ClassLoadingStrategy.Default.INJECTION.load(classLoader, binaryRepresentations);
        Class<?> type = loaded.get(Foo.class.getName());
        assertThat(loaded.size(), is(1));
        assertThat(type.getClassLoader(), is(classLoader));
        assertThat(type.getName(), is(Foo.class.getName()));
    }
}
