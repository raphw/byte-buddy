package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class MethodExtractorTest {

    @SuppressWarnings("unused")
    public static interface Foo {

        void foo();
    }

    @SuppressWarnings("unused")
    public static interface Bar extends Foo {

        void bar();
    }

    @SuppressWarnings("unused")
    public static class Baz {

        public void baz() {
            /* empty */
        }
    }

    @Test
    public void testSingleExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Object.class));
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Object.class.getDeclaredMethods()));
    }

    @Test
    public void testInheritedExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Baz.class));
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Baz.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Object.class.getDeclaredMethods()));
        assertThat(methods, hasItems(Baz.class.getDeclaredMethods()));
    }

    @Test
    public void testDoubleExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Object.class, Object.class));
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Object.class.getDeclaredMethods()));
    }

    @Test
    public void getInterfaceOnlyExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Foo.class));
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Foo.class.getDeclaredMethods()));
    }

    @Test
    public void getSingleInterfaceExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Object.class, Foo.class));
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Object.class.getDeclaredMethods()));
        assertThat(methods, hasItems(Foo.class.getDeclaredMethods()));
    }

    @Test
    public void getInheritedInterfaceExtraction() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.any());
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Object.class, Bar.class));
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length + Bar.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Object.class.getDeclaredMethods()));
        assertThat(methods, hasItems(Foo.class.getDeclaredMethods()));
        assertThat(methods, hasItems(Bar.class.getDeclaredMethods()));
    }

    @Test
    public void getInheritedInterfaceExtractionWithMatcher() {
        MethodExtractor methodExtractor = new MethodExtractor(MethodMatchers.declaredIn(Foo.class));
        List<Method> methods = methodExtractor.extractAll(Arrays.<Class<?>>asList(Object.class, Bar.class));
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(Foo.class.getDeclaredMethods()));
    }
}
