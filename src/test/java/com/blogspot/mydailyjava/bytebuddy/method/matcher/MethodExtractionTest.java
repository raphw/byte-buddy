package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class MethodExtractionTest {

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
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<MethodDescription> methods = methodExtraction.extractFrom(Object.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
    }

    @Test
    public void testInheritedExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<MethodDescription> methods = methodExtraction.extractFrom(Baz.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Baz.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Baz.class)));
    }

    @Test
    public void getInterfaceOnlyExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<MethodDescription> methods = methodExtraction.extractFrom(Foo.class).asList();
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    @Test
    public void getSingleInterfaceExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<MethodDescription> methods = methodExtraction.extractFrom(Object.class).appendInterface(Foo.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    @Test
    public void getInheritedInterfaceExtraction() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.any());
        List<MethodDescription> methods = methodExtraction.extractFrom(Object.class).appendInterface(Bar.class).asList();
        assertThat(methods.size(), is(Object.class.getDeclaredMethods().length + Foo.class.getDeclaredMethods().length + Bar.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Object.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Bar.class)));
    }

    @Test
    public void getInheritedInterfaceExtractionWithMatcher() {
        MethodExtraction methodExtraction = new MethodExtraction(MethodMatchers.declaredIn(Foo.class));
        List<MethodDescription> methods = methodExtraction.extractFrom(Object.class).appendInterface(Bar.class).asList();
        assertThat(methods.size(), is(Foo.class.getDeclaredMethods().length));
        assertThat(methods, hasItems(getDeclaredJavaMethods(Foo.class)));
    }

    private static MethodDescription[] getDeclaredJavaMethods(Class<?> type) {
        MethodDescription[] methodDescriptions = new MethodDescription[type.getDeclaredMethods().length];
        int i = 0;
        for(Method method : type.getDeclaredMethods()) {
            methodDescriptions[i++] = new MethodDescription.ForMethod(method);
        }
        return methodDescriptions;
    }
}
